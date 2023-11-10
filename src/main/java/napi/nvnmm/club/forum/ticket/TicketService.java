package napi.nvnmm.club.forum.ticket;


import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.util.exception.DataNotFoundException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TicketService {

    private final nAPI api;

    @Getter
    private final LoadingCache<String, ForumTicket> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ForumTicket>() {
                @Override
                public ForumTicket load(String id) throws DataNotFoundException {
                    Document document = api.getMongoService().getForumTickets()
                            .find(Filters.eq("id", id)).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new ForumTicket(document);
                }
            });

    public Optional<ForumTicket> getTicket(String id) {
        try {
            return Optional.ofNullable(cache.get(id));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveTicket(ForumTicket ticket) {
        api.getMongoService().getForumTickets().replaceOne(
                Filters.eq("id", ticket.getId()),
                ticket.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(ticket.getId(), ticket);
    }

    public void deleteTicket(ForumTicket ticket) {
        api.getMongoService().getForumTickets().deleteOne(Filters.eq("id", ticket.getId()));
        cache.asMap().remove(ticket.getId());
    }

    public List<ForumTicket> getPlayerTickets(UUID uuid, int page) {
        List<ForumTicket> tickets = new ArrayList<>();
        api.getMongoService().getForumTickets().find(Filters.eq("author", uuid.toString()))
                .forEach((Block<? super Document>) document -> tickets.add(new ForumTicket(document)));
        return tickets;
    }

    public List<ForumTicket> getAllTickets(int page) {
        List<ForumTicket> tickets = new ArrayList<>();
        api.getMongoService().getForumTickets().find()
                .forEach((Block<? super Document>) document -> tickets.add(new ForumTicket(document)));
        return tickets;
    }
}
