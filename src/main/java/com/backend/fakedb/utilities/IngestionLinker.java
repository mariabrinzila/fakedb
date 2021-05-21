package com.backend.fakedb.utilities;

import com.backend.fakedb.entities.AiEntity;
import com.backend.fakedb.entities.IngestionEntity;
import com.backend.fakedb.entities.PostEntity;
import com.backend.fakedb.entities.ProviderEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that handles the communication between the ingestion system and this app.
 *
 * It requests the posts from the ingestion system then it splits every post into two:
 * an AiEntity (which will be sent to the AI module) and a PostEntity which will be constructed
 * with information from the ingestion and the examination result (how likely it is to be fake news).
 */
public class IngestionLinker {

    RestTemplate ingestion;

    public IngestionLinker() {
        ingestion = new RestTemplate();
    }

    /**
     * Public method for getting a list of PostEntity objects from the posts database.
     * If the given arguments surpass the amount of rows in the database, null objects will be added to the list.
     * @param skip the amount of rows to skip (must be a number greater than or equal to zero)
     * @param count the amount of rows to receive (must be a number greater than zero)
     * @return the specified list
     */
    public List<PostEntity> getInterval(int skip, int count) {
        if (skip < 0 || count < 1) {
            return null;
        }

        // The response will return posts from the database, according to the skip and count parameters
        ResponseEntity<IngestionEntity[]> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/getInterval?skip=" + skip + "&count=" + count, IngestionEntity[].class);
        IngestionEntity[] ingestionArray = response.getBody();

        // If the list is null or the skip is too big, don't continue.
        if (ingestionArray == null || skip >= ingestionArray.length) {
            return null;
        }

        List<PostEntity> responseList = new ArrayList<>(ingestionArray.length);

        for (int i = 0; i < ingestionArray.length; i++) {
            // We need to sent an AiEntity to the AI module and get the associated result
            ScoreResult responseScore = ingestion.postForObject("https://ai-communication-module.herokuapp.com/ai-module/", convertToAiEntity(ingestionArray[i]), ScoreResult.class);

            // We need to build a PostEntity with the associated score and information
            assert responseScore != null;
            responseList.add(i, convertToPostEntity(ingestionArray[i], responseScore.getScore()));
        }
        return responseList;
    }

    /**
     * Public method for getting a list of posts by a specific provider.
     *  If the given arguments surpass the amount of rows in the database, null objects will be added to the list.
     * @param providerId the provider ID to get posts from
     * @param skip the amount of posts to be skipped (needs to be greater than or equal to zero)
     * @param count the number of posts to receive (needs to be greater that zero)
     * @return the specified list
     */
    public List<PostEntity> getIntervalByProvider(int providerId, int skip, int count) {
        if (skip < 0 || count < 1) {
            return null;
        }

        // The response will return posts from the database provided by provider_id, according to the skip and count parameters
        ResponseEntity<IngestionEntity[]> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/getIntervalByProvider?provider_id=" + providerId + "&skip=" + skip + "&count=" + count, IngestionEntity[].class);
        IngestionEntity[] ingestionArray = response.getBody();

        // If the list is null or the skip is too big, don't continue.
        if (ingestionArray == null || skip >= ingestionArray.length) {
            return null;
        }

        List<PostEntity> responseList = new ArrayList<>(ingestionArray.length);

        for (int i = 0; i < ingestionArray.length; i++) {
            // We need to sent an AiEntity to the AI module and get the associated result
            ScoreResult responseScore = ingestion.postForObject("https://ai-communication-module.herokuapp.com/ai-module/", convertToAiEntity(ingestionArray[i]), ScoreResult.class);

            // We need to build a PostEntity with the associated score and information
            assert responseScore != null;
            responseList.add(i, convertToPostEntity(ingestionArray[i], responseScore.getScore()));
        }
        return responseList;
    }

    /**
     * Private method that converts a given ingestion entity to a AI entity.
     * @param ingestionEntity the entity to be converted
     * @return the AI entity
     */
    private AiEntity convertToAiEntity(IngestionEntity ingestionEntity) {
        return new AiEntity(ingestionEntity.getId().hashCode(),
                            ingestionEntity.getTitle(),
                            ingestionEntity.getContent());
    }

    /**
     * Private method that converts a given ingestion entity to a Post entity.
     * @param ingestionEntity the entity to be converted
     * @param score the associated score for this specific post
     * @return the Post entity
     */
    private PostEntity convertToPostEntity(IngestionEntity ingestionEntity, String score) {
        return new PostEntity(ingestionEntity.getId().hashCode(),
                                ingestionEntity.getTitle(),
                                ingestionEntity.getThumbnail(),
                                ingestionEntity.getDescription(),
                                ingestionEntity.getPostDate(),
                                score,
                                ingestionEntity.getSourceUrl());
    }

    /**
     * Public method for getting a list of all the ProviderEntity objects from the posts database.
     * @return the list
     */
    public List<ProviderEntity> provider_getAll() {

        // The response will return all providers from the database
        ResponseEntity<ProviderEntity[]> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/getAll", ProviderEntity[].class);
        ProviderEntity[] providerArray = response.getBody();

        assert providerArray != null;
        return new ArrayList<ProviderEntity>(Arrays.asList(providerArray));
    }

    /**
     * Public method for getting a list of ProviderEntity objects from the posts database.
     * If the given arguments surpass the amount of rows in the database, null objects will be added to the list.
     * @param skip the amount of rows to skip (must be a number greater than or equal to zero)
     * @param count the amount of rows to receive (must be a number greater than zero)
     * @return the specified list
     */
    public List<ProviderEntity> provider_getInterval(int skip, int count) {
        if (skip < 0 || count < 1) {
            return null;
        }

        // The response will return providers from the database, according to the skip and count parameters
        ResponseEntity<ProviderEntity[]> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/providers/getInterval?skip=" + skip + "&count=" + count, ProviderEntity[].class);
        ProviderEntity[] providerArray = response.getBody();

        // If the list is null or the skip is too big, don't continue.
        if (providerArray == null || skip >= providerArray.length) {
            return null;
        }

        return new ArrayList<ProviderEntity>(Arrays.asList(providerArray));
    }

    /**
     * Public method for getting count of all the providers.
     * @return the count
     */
    public IntWrapper provider_getCount() {

        // The response will return all providers from the database
        ResponseEntity<IntWrapper> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/getCount", IntWrapper.class);

        return response.getBody();
    }

    /**
     * public method for getting the count for providers matching the query
     * @return  the count
     * */

    public IntWrapper provider_searchCount(String queryToBeContained){
        ResponseEntity<IntWrapper> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/searchCount?query=" + queryToBeContained, IntWrapper.class);
        return response.getBody();
    }

    /**
     * Public method for getting a list of ProviderEntity objects from the posts database by specified filters.
     * If the given arguments surpass the amount of rows in the database, null objects will be added to the list.
     * @param query filters for the searching of providers
     * @param skip the amount of rows to skip (must be a number greater than or equal to zero)
     * @param count the amount of rows to receive (must be a number greater than zero)
     * @return the specified list
     */

    public List<ProviderEntity> provider_search(String query, int skip, int count){
        if (skip < 0 || count < 1) {
            return null;
        }

        ResponseEntity<ProviderEntity[]> response =
                ingestion.getForEntity("https://fake-news-detection-ingestion.herokuapp.com/v1/api/news/search?query="+ query + "&skip=" + skip + "&count=" + count, ProviderEntity[].class);
        ProviderEntity[] providerArray = response.getBody();

        assert providerArray != null;
        return new ArrayList<ProviderEntity>(Arrays.asList(providerArray));
    }


}
