package com.github;

import java.sql.SQLException;

public interface CrawlerDao {
    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    void updateDatabase(String href, String s) throws SQLException;

    String getOneLinkAndDeleteIt() throws SQLException;

    String loadaLinksFromDataBase(String sql) throws SQLException;

}
