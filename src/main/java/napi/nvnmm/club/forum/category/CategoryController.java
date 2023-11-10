package napi.nvnmm.club.forum.category;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "/forum/category")
public class CategoryController {

    private final nAPI api;
    private final CategoryService categoryService;

    public CategoryController(nAPI api) {
        this.api = api;
        this.categoryService = api.getForumService().getCategoryService();
    }

    @GetMapping
    public ResponseEntity<JsonElement> getCategories() {
        JsonArray array = new JsonArray();
        api.getForumService().getCategoryService().getCategories().forEach(category -> array.add(category.toJson()));
        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JsonElement> createCategory(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        String name = body.get("name").getAsString();

        Optional<ForumCategory> categoryOpt = categoryService.getByIdOrName(id);
        if (categoryOpt.isPresent()) {
            response.add("message", "Category already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        categoryOpt = categoryService.getByIdOrName(name);
        if (categoryOpt.isPresent()) {
            response.add("message", "Category already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumCategory category = new ForumCategory();
        category.setId(id);
        category.setName(name);
        category.setWeight(body.get("weight").getAsInt());

        categoryService.saveCategory(category);

        return new ResponseEntity<>(category.toJson(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> updateCategory(@RequestBody JsonObject body, @PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumCategory> categoryOpt = categoryService.getById(id);
        if (!categoryOpt.isPresent()) {
            response.add("message", "Category not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumCategory category = categoryOpt.get();
        if (body.has("name"))
            category.setName(body.get("name").getAsString());

        if (body.has("weight"))
            category.setWeight(body.get("weight").getAsInt());

        categoryService.saveCategory(category);

        return new ResponseEntity<>(category.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<JsonElement> deleteCategory(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumCategory> categoryOpt = categoryService.getById(id);
        if (!categoryOpt.isPresent()) {
            response.add("message", "Category not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumCategory category = categoryOpt.get();
        categoryService.deleteCategory(category);
        return new ResponseEntity<>(category.toJson(), HttpStatus.OK);
    }


}
