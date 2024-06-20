package com.microsoftTeams.bot.helpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MergeRequest class which is used to store data of the event type merge_request in webhooks Gitlab
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequest {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("iid")
    private Long iid;

    @JsonProperty("title")
    private String title;

    @JsonProperty("last_commit")
    private LastCommit lastCommit;

    @JsonProperty("state")
    private String state;

    @JsonProperty("merge_status")
    private String mergeStatus;

    @JsonProperty("has_conflicts")
    private String hasConflicts;

    @JsonProperty("web_url")
    private String webUrl;

    @JsonProperty("author")
    private Author author;


    public MergeRequest() {
    }


    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public String getHasConflicts() {
        return hasConflicts;
    }

    public void setHasConflicts(String hasConflicts) {
        this.hasConflicts = hasConflicts;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
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

    public LastCommit getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(LastCommit lastCommit) {
        this.lastCommit = lastCommit;
    }

    public Long getIid() {
        return iid;
    }

    public void setIid(Long iid) {
        this.iid = iid;
    }
}
