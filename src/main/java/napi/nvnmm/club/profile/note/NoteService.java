package napi.nvnmm.club.profile.note;

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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class NoteService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, Note> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, Note>() {
                @Override
                public Note load(UUID id) throws DataNotFoundException {
                    Document document = api.getMongoService().getNotes()
                            .find(Filters.eq("id", id.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new Note(document);
                }
            });

    @Getter
    private final LoadingCache<UUID, List<Note>> playerCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, List<Note>>() {
                @Override
                public List<Note> load(UUID uuid) {
                    List<Note> notes = new ArrayList<>();
                    api.getMongoService().getNotes().find(Filters.eq("uuid", uuid.toString()))
                            .forEach((Block<? super Document>) document -> {
                                Note note = new Note(document);
                                cache.put(note.getId(), note);
                                notes.add(note);
                            });

                    return notes;
                }
            });

    public Optional<Note> getNote(UUID id) {
        try {
            return Optional.ofNullable(cache.get(id));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Note> getNotesOf(UUID uuid) {
        try {
            return playerCache.get(uuid);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void saveNote(Note note) {
        api.getMongoService().getNotes().replaceOne(
                Filters.eq("id", note.getId().toString()),
                note.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(note.getId(), note);
        playerCache.refresh(note.getUuid());
    }

    public void deleteNote(Note note) {
        api.getMongoService().getNotes().deleteOne(Filters.eq("id", note.getId().toString()));
        cache.refresh(note.getId());
        playerCache.refresh(note.getUuid());
    }

}
