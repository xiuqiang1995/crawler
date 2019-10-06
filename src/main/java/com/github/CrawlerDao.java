package com.github;

import java.sql.SQLException;

public interface CrawlerDao {
    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    String getOneLinkAndDeleteIt() throws SQLException;

    void insertLinkToBeProcessed(String href);

    void insertProcessedLink(String link);
}
