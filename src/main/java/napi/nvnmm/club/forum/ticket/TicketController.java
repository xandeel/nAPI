package napi.nvnmm.club.forum.ticket;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/forum/ticket")
public class TicketController {

    private final nAPI api;
    private final TicketService ticketService;

    public TicketController(nAPI api) {
        this.api = api;
        this.ticketService = api.getForumService().getTicketService();
    }

    @PostMapping
    public ResponseEntity<JsonElement> createTicket(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        Optional<ForumTicket> ticketOpt = ticketService.getTicket(id);
        if (ticketOpt.isPresent()) {
            response.add("message", "Ticket already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumTicket ticket = new ForumTicket();
        ticket.setId(id);
        ticket.setCategory(body.get("category").getAsString());
        ticket.setBody(body.get("body").getAsString());
        ticket.setStatus(body.get("status").getAsString());
        ticket.setAuthor(UUID.fromString(body.get("author").getAsString()));
        ticket.setCreatedAt(System.currentTimeMillis());
        ticket.setLastUpdatedAt(System.currentTimeMillis());

        ticketService.saveTicket(ticket);

        return new ResponseEntity<>(ticket.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> editTicket(@RequestBody JsonObject body, @PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumTicket> ticketOpt = ticketService.getTicket(id);
        if (!ticketOpt.isPresent()) {
            response.add("message", "Ticket not found.");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumTicket ticket = ticketOpt.get();

        if (body.has("category"))
            ticket.setCategory(body.get("title").getAsString());

        if (body.has("body"))
            ticket.setBody(body.get("body").getAsString());

        if (body.has("status"))
            ticket.setStatus(body.get("status").getAsString());

        ticket.setLastUpdatedAt(System.currentTimeMillis());

        ticketService.saveTicket(ticket);
        return new ResponseEntity<>(ticket.toJson(), HttpStatus.OK);
    }


    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getTicket(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumTicket> ticketOpt = ticketService.getTicket(id);
        if (!ticketOpt.isPresent()) {
            response.add("message", "Ticket not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(ticketOpt.get().toJson(), HttpStatus.OK);
    }

    @GetMapping(path = "/player/{uuid}")
    public ResponseEntity<JsonElement> getPlayerTickets(@PathVariable(name = "uuid") UUID uuid,
                                                        @RequestParam(name = "page", defaultValue = "1") int page) {
        JsonArray array = new JsonArray();

        List<ForumTicket> tickets = ticketService.getPlayerTickets(uuid, page);
        tickets.forEach(ticket -> array.add(ticket.toJson()));

        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    // makes this /forum/tickets/all, clashing with /forum/ticket/{id} otherwise
    @GetMapping(path = "s/all")
    public ResponseEntity<JsonElement> getAllTickets(@RequestParam(name = "page", defaultValue = "1") int page) {
        JsonArray array = new JsonArray();

        List<ForumTicket> tickets = ticketService.getAllTickets(page);
        tickets.forEach(ticket -> array.add(ticket.toJson()));

        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    @PostMapping(path = "/{parentId}/reply")
    public ResponseEntity<JsonElement> createReply(@RequestBody JsonObject body,
                                                   @PathVariable(name = "parentId") String parentId) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumTicket> parentOpt = ticketService.getTicket(parentId);
        if (!parentOpt.isPresent()) {
            response.add("message", "Parent ticket not found");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumTicket parent = parentOpt.get();
        TicketReply reply = new TicketReply();

        reply.setId(body.get("id").getAsString());
        reply.setBody(body.get("body").getAsString());
        reply.setAuthor(UUID.fromString(body.get("author").getAsString()));
        reply.setCreatedAt(System.currentTimeMillis());
        reply.setParentTicketId(parent.getId());

        parent.setLastUpdatedAt(System.currentTimeMillis());
        parent.getReplies().add(reply);

        ticketService.saveTicket(parent);
        return new ResponseEntity<>(reply.toJson(), HttpStatus.OK);
    }
}
