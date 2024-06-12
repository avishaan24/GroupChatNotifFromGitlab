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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This controller will receive GET requests at /api/notify and send a message
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
    private final ConcurrentHashMap<Long, Author> authorList;


    private ConversationReferences conversationReferences;
    private String appId;

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
        if(information.getObject_kind().equals("merge_request")){
            if(dictionary.get(information.getObject_attributes().getId()) == null){
                dictionary.put(information.getObject_attributes().getId(), information.getObject_attributes().getLast_commit().getAuthor());
                authorList.put(information.getObject_attributes().getAuthor_id(), information.getObject_attributes().getLast_commit().getAuthor());
            }
            Author lastCommitAuthor = information.getObject_attributes().getLast_commit().getAuthor();
            Author author = dictionary.get(information.getObject_attributes().getId());
            if(information.getObject_attributes().getMerge_status().equals("can_be_merged") && information.getObject_attributes().getState().equals("opened")){
                heroCard.setTitle("Merge Notification");
                heroCard.setText("Merge commited by " + author.getName() + " is ready to merge");
                heroCard.setButtons(new CardAction(ActionTypes.OPEN_URL, "View Merge Request", information.getObject_attributes().getUrl()));
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
            else if(information.getObject_attributes().getMerge_status().equals("checking")){
                CompletableFuture<MergeRequest> mergeRequestFuture = GroupChatGitlabBot.waitForMergeStatus(information.getProject().getId(), information.getObject_attributes().getId(), accessToken);
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
                System.out.println(mergeRequest.getMerge_status());
                sendNotification(mergeRequest);
            }
        }
        else if(information.getObject_kind().equals("pipeline")) {
            if (information.getObject_attributes().getStatus().equals("success")) {
                CompletableFuture<MergeRequest> mergeRequestFuture = GroupChatGitlabBot.waitForMergeRequest(information.getProject().getId(), information.getObject_attributes().getSha(), accessToken);
                AtomicReference<MergeRequest> localMergeRequest = new AtomicReference<>(null); // Use AtomicReference

                CompletableFuture<Void> resultHandler = mergeRequestFuture.thenAccept(mergeRequest -> {
                    if (mergeRequest != null) {
                        // Save the merge request to the AtomicReference
                        localMergeRequest.set(mergeRequest);
                    } else {
                        System.out.println("Failed to receive Merge Request");
                    }
                });

                //wait for the resultHandler to ensure that the merge request handling is complete
                resultHandler.join();

                //use the localMergeRequest.get() to retrieve the MergeRequest
                MergeRequest mergeRequest = localMergeRequest.get();
                sendNotification(mergeRequest);
            }
        }
    }

    /**
     * Send notification to all the groups in which author of mergeRequest user belongs
     * @param mergeRequest
     */
    public void sendNotification(MergeRequest mergeRequest){
        HeroCard heroCard = new HeroCard();
        if (mergeRequest != null) {
            if(mergeRequest.getMerge_status().equals("can_be_merged") && mergeRequest.getHas_conflicts().equals("false") && mergeRequest.getState().equals("opened")){
                Author author = authorList.get(mergeRequest.getAuthor().getId());
                heroCard.setTitle("Merge Notification");
                heroCard.setText("Merge commited by " + author.getName() + " is ready to merge");
                heroCard.setButtons(new CardAction(ActionTypes.OPEN_URL, "View Merge Request", mergeRequest.getWeb_url()));
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
