package com.microsoftTeams.bot;

import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.integration.BotFrameworkHttpAdapter;
import com.microsoft.bot.integration.Configuration;
import com.microsoft.bot.schema.ActionTypes;
import com.microsoft.bot.schema.CardAction;
import com.microsoft.bot.schema.ConversationReference;
import com.microsoft.bot.schema.HeroCard;
import com.microsoftTeams.bot.helpers.Author;

import com.microsoftTeams.bot.helpers.MergeRequest;
import com.microsoftTeams.bot.helpers.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This controller will receive POST requests at /api/Gitlab/groupNotify and send a message
 * to all ConversationReferences.
 *
 * @see ConversationReferences
 * @see GroupChatGitlabBot
 * @see Application
 */
@RestController
public class NotifyController {
    /**
     * The BotFrameworkHttpAdapter to use. Note is provided by dependency
     * injection via the constructor.
     *
     * @see com.microsoft.bot.integration.spring.BotDependencyConfiguration
     */
    private final BotFrameworkHttpAdapter adapter;

    // which stores author who creates PR, comments and initiate pipeline
    private final ConcurrentHashMap<Long, Author> dictionary;

    // stores author with key as id of the author
    private final ConcurrentHashMap<Long, Author> authorList;


    private final ConversationReferences conversationReferences;
    private final String appId;

    @Value("${accessToken}")
    private String accessToken;

    @Autowired
    public NotifyController(
        BotFrameworkHttpAdapter withAdapter,
        Configuration withConfiguration,
        ConversationReferences withReferences
    ) {
        adapter = withAdapter;
        conversationReferences = withReferences;
        appId = withConfiguration.getProperty("MicrosoftAppId");
        this.dictionary = new ConcurrentHashMap<>();
        this.authorList = new ConcurrentHashMap<>();
    }

    @PostMapping("/api/Gitlab/groupNotify")
    public void sendNotificationToTheGroup(@RequestBody Information information){
        HeroCard heroCard = new HeroCard();
        switch (information.getObjectKind()) {
            case "merge_request":
                if (dictionary.get(information.getObjectAttributes().getId()) == null) {
                    dictionary.put(information.getObjectAttributes().getId(), information.getObjectAttributes().getLastCommit().getAuthor());
                    authorList.put(information.getObjectAttributes().getAuthorId(), information.getObjectAttributes().getLastCommit().getAuthor());
                }

                Author author = dictionary.get(information.getObjectAttributes().getId());
                if(information.getObjectAttributes().getMergeStatus().equals("can_be_merged") && information.getObjectAttributes().getState().equals("opened")) {
                    heroCard.setTitle("Merge Notification");
                    heroCard.setText("Merge commited by " + author.getName() + " is ready to merge");
                    heroCard.setButtons(new CardAction(ActionTypes.OPEN_URL, "View Merge Request", information.getObjectAttributes().getUrl()));
                    if (!conversationReferences.isEmpty()) {
                        for (ConversationReference conversationReference : conversationReferences.get("avinash")) {
                            adapter.continueConversation(
                                    appId, conversationReference, turnContext -> turnContext.sendActivity(MessageFactory.attachment(heroCard.toAttachment())).thenApply(resourceResponse -> null)
                            );
                        }
                    } else {
                        System.out.println("Empty");
                    }
                } else if (information.getObjectAttributes().getMergeStatus().equals("checking")) {

                    // firstly check the status of the pipeline associated with the merge request
                    CompletableFuture<Pipeline> pipelineFuture = GroupChatGitlabBot.waitForPipelineStatus(information.getProject().getId(), information.getObjectAttributes().getHeadPipelineId(), accessToken);
                    AtomicReference<Pipeline> localPipeline = new AtomicReference<>(null); // Use AtomicReference
                    CompletableFuture<Void> resultHandler = pipelineFuture.thenAccept(pipeline -> {
                        if (pipeline != null) {
                            // Save the pipeline to the AtomicReference
                            localPipeline.set(pipeline);
                        } else {
                            System.out.println("Failed to receive Pipeline");
                        }
                    });

                    // wait for the resultHandler to ensure that the merge request handling is complete
                    resultHandler.join();

                    // use the localPipeline.get() to retrieve the pipeline
                    Pipeline pipeline = localPipeline.get();

                    // if it is success then check the merge Request is ready to merge or not
                    if(pipeline!= null && pipeline.getStatus().equals("success")){
                        CompletableFuture<MergeRequest> mergeRequestFuture = GroupChatGitlabBot.waitForMergeStatus(information.getProject().getId(), information.getObjectAttributes().getIid(), accessToken);
                        sendNotification(mergeRequestFuture);
                    }
                }
                break;
            case "pipeline":
                if (information.getObjectAttributes().getStatus().equals("success")) {
                    // fetch merge Request associated with the pipeline and then notify user
                    CompletableFuture<MergeRequest> mergeRequestFuture = GroupChatGitlabBot.waitForMergeRequest(information.getProject().getId(), information.getObjectAttributes().getSha(), accessToken);
                    sendNotification(mergeRequestFuture);
                }
                break;
        }
    }

    /**
     * Send notification to all the groups in which author of mergeRequest user belongs
     * @param mergeRequestFuture
     */
    public void sendNotification(CompletableFuture<MergeRequest> mergeRequestFuture){
        AtomicReference<MergeRequest> localMergeRequest = new AtomicReference<>(null); // Use AtomicReference
        CompletableFuture<Void> resultHandler = mergeRequestFuture.thenAccept(mergeRequest -> {
            if (mergeRequest != null) {
                // Save the merge request to the AtomicReference
                localMergeRequest.set(mergeRequest);
            } else {
                System.out.println("Failed to receive Merge Request");
            }
        });

        // wait for the resultHandler to ensure that the merge request handling is complete
        resultHandler.join();

        // use the localMergeRequest.get() to retrieve the MergeRequest
        MergeRequest mergeRequest = localMergeRequest.get();
        HeroCard heroCard = new HeroCard();
        if (mergeRequest != null) {
            if(mergeRequest.getMergeStatus().equals("can_be_merged") && mergeRequest.getHasConflicts().equals("false") && mergeRequest.getState().equals("opened")){
                Author author = authorList.get(mergeRequest.getAuthor().getId());
                heroCard.setTitle("Merge Notification");
                heroCard.setText("Merge commited by " + author.getName() + " is ready to merge");
                heroCard.setButtons(new CardAction(ActionTypes.OPEN_URL, "View Merge Request", mergeRequest.getWebUrl()));
                if(!conversationReferences.isEmpty()){
                    for(ConversationReference conversationReference: conversationReferences.get("avinash")){
                        adapter.continueConversation(
                                appId, conversationReference, turnContext -> turnContext.sendActivity(MessageFactory.attachment(heroCard.toAttachment())).thenApply(resourceResponse -> null)
                        );
                    }
                }
                else{
                    System.out.println("Empty");
                }
            }
        }
    }
}
