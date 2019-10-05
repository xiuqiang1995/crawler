package com.github;

import java.sql.SQLException;

public class MybatisCrawlerDao implements CrawlerDao {
    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        return false;
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {

    }

    @Override
    public void updateDatabase(String href, String s) throws SQLException {

    }

    @Override
    public String getOneLinkAndDeleteIt() throws SQLException {
        return null;
    }

    @Override
    public String loadaLinksFromDataBase(String sql) throws SQLException {
        return null;
    }
}
