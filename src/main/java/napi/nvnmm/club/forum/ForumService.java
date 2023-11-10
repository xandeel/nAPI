package napi.nvnmm.club.forum;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.account.AccountService;
import napi.nvnmm.club.forum.category.CategoryService;
import napi.nvnmm.club.forum.forum.ForumModelService;
import napi.nvnmm.club.forum.thread.ThreadService;
import napi.nvnmm.club.forum.ticket.TicketService;
import napi.nvnmm.club.forum.trophy.TrophyService;
import lombok.Getter;

@Getter
public class ForumService {

    private final nAPI api;

    private final AccountService accountService;
    private final CategoryService categoryService;
    private final ForumModelService forumModelService;
    private final ThreadService threadService;
    private final TicketService ticketService;
    private final TrophyService trophyService;

    public ForumService(nAPI api) {
        this.api = api;
        this.accountService = new AccountService(api);

        this.categoryService = new CategoryService(api);
        categoryService.loadCategories();

        this.forumModelService = new ForumModelService(api);
        forumModelService.loadForums();

        this.threadService = new ThreadService(api);
        this.ticketService = new TicketService(api);

        this.trophyService = new TrophyService(api);
        trophyService.loadTrophies();
    }
}
