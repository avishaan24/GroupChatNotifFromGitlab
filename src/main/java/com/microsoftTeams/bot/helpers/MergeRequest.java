package com.microsoftTeams.bot.helpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MergeRequest class which is used to store data of the event type merge_request in webhooks Gitlab
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequest {
    private Long id;
    private String title;
    private LastCommit last_commit;
    private String state;
    private String merge_status;
    private String has_conflicts;
    private String web_url;

    @JsonProperty("author")
    private Author author;

    private int upvote;

    public MergeRequest() {
    }

    public int getUpvote() {
        return upvote;
    }

    public void setUpvote(int upvote) {
        this.upvote = upvote;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getWeb_url() {
        return web_url;
    }

    public void setWeb_url(String web_url) {
        this.web_url = web_url;
    }

    public String getMerge_status() {
        return merge_status;
    }

    public String getHas_conflicts() {
        return has_conflicts;
    }

    public void setHas_conflicts(String has_conflicts) {
        this.has_conflicts = has_conflicts;
    }

    public void setMerge_status(String merge_status) {
        this.merge_status = merge_status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LastCommit getLast_commit() {
        return last_commit;
    }

    public void setLast_commit(LastCommit last_commit) {
        this.last_commit = last_commit;
    }
}
