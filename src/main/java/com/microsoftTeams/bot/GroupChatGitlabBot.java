package com.microsoftTeams.bot;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsInfo;
import com.microsoft.bot.schema.*;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.microsoftTeams.bot.helpers.MergeRequest;
import com.microsoftTeams.bot.helpers.Pipeline;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class implements the functionality of the Bot.
 *
 * <p>
 * This is where application specific logic for interacting with the users would
 * be added. In this bot, the {@link #onMessageActivity(TurnContext)}
 * updates the shared
 * {@link ConversationReferences}. The
 * {@link #onMembersAdded(List, TurnContext)} will send a greeting to new
 * conversation participants with instructions for sending a proactive message.
 * </p>
 */
public class GroupChatGitlabBot extends ActivityHandler {
    @Value("${server.port:3978}")
    private int port;

    // Message to send to users when the bot receives a Conversation Update event
    private final String welcomeMessage =
        "Successfully added, we will notify about your operations in Gitlab.\n" + "\nThanks!!";

    private final ConversationReferences conversationReferences;

    public GroupChatGitlabBot(ConversationReferences withReferences) {
        conversationReferences = withReferences;
    }

    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        addConversationReference(turnContext);
        return null;
    }

    @Override
    protected CompletableFuture<Void> onMembersAdded(
        List<ChannelAccount> membersAdded,
        TurnContext turnContext
    ) {
        return membersAdded.stream()
            .filter(
                // Greet anyone that was not the target (recipient) of this message.
                member -> !StringUtils
                    .equals(member.getId(), turnContext.getActivity().getRecipient().getId())
            )
            .map(
                channel -> turnContext
                    .sendActivity(MessageFactory.text(welcomeMessage))
            )
            .collect(CompletableFutures.toFutureList())
            .thenApply(resourceResponses -> null);
    }

    @Override
    protected CompletableFuture<Void> onConversationUpdateActivity(TurnContext turnContext) {
        addConversationReference(turnContext);
        return super.onConversationUpdateActivity(turnContext);
    }

    // adds a ConversationReference to the shared Map.
    private void addConversationReference(TurnContext turnContext) {
        ConversationReference conversationReference = turnContext.getActivity().getConversationReference();
        TeamsInfo.getMembers(turnContext).thenAccept(members -> {
            // Process the list of members
            for (TeamsChannelAccount member : members) {
                if(!member.getName().equals("Bot")){
                    boolean present = false;
                    conversationReferences.computeIfAbsent(member.getId(), k -> new ArrayList<>());

                    for(int i = 0; i < conversationReferences.get(member.getId()).size(); i++){
                        if(conversationReferences.get(member.getId()).get(i).getConversation().getId().equals(turnContext.getActivity().getConversationReference().getConversation().getId())){
                            conversationReferences.get(member.getId()).set(i, turnContext.getActivity().getConversationReference());
                            present = true;
                        }
                    }
                    if(!present){
                        conversationReferences.get(member.getId()).add(turnContext.getActivity().getConversationReference());
                    }
                }
            }
        });
    }

    /**
     * Fetch merge request related to the pipeline
     * @param projectId
     * @param sha
     * @param accessToken
     * @return mergeRequest
     */
    public static CompletableFuture<MergeRequest> waitForMergeRequest(String projectId, String sha, String accessToken) {
        String url = "https://gitlab.com/api/v4/projects/" + projectId + "/repository/commits/" + sha + "/merge_requests";
        return fetchMergeRequest(url, accessToken);
    }

    /**
     * fetch merge request with the help of id
     * @param projectId
     * @param id
     * @param accessToken
     * @return mergeRequest
     */
    public static CompletableFuture<MergeRequest> waitForMergeStatus(String projectId, Long id, String accessToken) {

        String url = "https://gitlab.com/api/v4/projects/" + projectId + "/merge_requests?id=" + id.toString();
        return fetchMergeRequest(url, accessToken);
    }

    public static CompletableFuture<Pipeline> waitForPipelineStatus(String projectId, String headPipelineId, String accessToken){
        String url = "https://gitlab.com/api/v4/projects/" + projectId + "/pipelines/" + headPipelineId;
        return fetchPipeline(url, accessToken);
    }

    /**
     * call gitlab api to fetch merge request
     * @param url
     * @param accessToken
     * @return
     */
    private static CompletableFuture<MergeRequest> fetchMergeRequest(String url, String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("PRIVATE-TOKEN", accessToken);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    // Read the response into a JsonNode
                    JsonNode rootNode = objectMapper.readTree(connection.getInputStream());

                    // Check if the root node is an array
                    if (rootNode.isArray() && !rootNode.isEmpty()) {
                        // Get the first element from the array
                        JsonNode mergeRequestNode = rootNode.get(0);
                        // Deserialize the JSON object into a MergeRequest object
                        MergeRequest mergeRequest = objectMapper.treeToValue(mergeRequestNode, MergeRequest.class);
                        return mergeRequest;
                    } else {
                        return null;
                    }
                } else {
                    System.err.println("Error: " + connection.getResponseMessage());
                    return null; // Return null if there was an error
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Return null in case of exception
            }
        });
    }

    /**
     * call gitlab api to fetch pipeline
     * @param url
     * @param accessToken
     * @return
     */
    private static CompletableFuture<Pipeline> fetchPipeline(String url, String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("PRIVATE-TOKEN", accessToken);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    // Read the response into a JsonNode
                    JsonNode rootNode = objectMapper.readTree(connection.getInputStream());

                    // Deserialize the JSON object into a MergeRequest object
                    Pipeline pipeline = objectMapper.treeToValue(rootNode, Pipeline.class);
                    return pipeline;
                } else {
                    System.err.println("Error: " + connection.getResponseMessage());
                    return null; // Return null if there was an error
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null; // Return null in case of exception
            }
        });
    }
}
