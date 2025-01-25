import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {
    private final List<Document> documents;
    private final AtomicInteger idGenerator = new AtomicInteger(0);
    private final AtomicInteger authorIdGenerator = new AtomicInteger(0);  

    public DocumentManager() {
        this.documents = new ArrayList<>();
    }
    public DocumentManager(List<Document> documents) {
        this.documents = documents;
    }

    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */
    public Document save(Document document) {
        if(document.getId() == null || findById(document.getId()).isEmpty()) {
            document.setId(generateDocumentId());
            if (document.getTitle().isEmpty()) {
                document.setTitle(generateTitle(document.getContent()));
            }
            document.setAuthor(getAuthorByName(document.getAuthor().getName()));
            document.setCreated(Instant.now());
            documents.add(document);
        } else {
            findById(document.getId()).ifPresent(d -> {
                d.setTitle(document.getTitle());
                d.setContent(document.getContent());
                d.setAuthor(document.getAuthor());
            });
        }
        return document;       
    }

    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {
        List<Document> result = documents;

        if (request.titlePrefixes != null && !request.titlePrefixes.isEmpty()) {
            result = result.stream()
                    .filter(d -> request.titlePrefixes.stream().anyMatch(prefix -> d.title.startsWith(prefix)))
                    .toList();
        }

        if (request.containsContents != null && !request.containsContents.isEmpty()) {
            result = result.stream()
                    .filter(d -> request.containsContents.stream().anyMatch(content -> d.content.contains(content)))
                    .toList();
        }

        if (request.authorIds != null && !request.authorIds.isEmpty()) {
            result = result.stream()
                    .filter(d -> request.authorIds.contains(d.author.getId()))
                    .toList();
        }

        if (request.createdTo != null || request.createdFrom != null) {
            result = result.stream()
                    .filter(d -> (request.createdFrom == null || d.created.toEpochMilli() >= request.createdFrom.toEpochMilli()) &&
                            (request.createdTo == null || d.created.toEpochMilli() <= request.createdTo.toEpochMilli())
                    )
                    .toList();

        }
        return result;
    }

    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {
        return documents.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst();
    }

    private String generateDocumentId() {
       return "DOC-" + idGenerator.incrementAndGet();
    }

    private String generateAuthorId(){
        return "AUTH-" + authorIdGenerator.incrementAndGet();
    }

    private String generateTitle(String content){
        if (content == null || content.isEmpty()) {
            return "Untitled";
        } else {
            return content.substring(0, Math.min(content.length(), 25));
        }
    }

    private Author getAuthorByName(String name) {
        return documents.stream()
                .map(Document::getAuthor)
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElse(Author.builder().id(generateAuthorId()).name(name).build());
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }
}