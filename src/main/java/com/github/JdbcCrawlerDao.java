package com.github;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 分离数据库操作
 */
public class JdbcCrawlerDao implements CrawlerDao{
    Connection connection;

    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:/Users/br/githubws/crawler-1/crawlerDB", "root", "root");
        } catch (SQLException e) {
           throw new RuntimeException(e);
        }
    }

    private String loadaLinkFromDataBase(String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }

    @Override
    public String getOneLinkAndDeleteIt() throws SQLException {
        //待处理链接池
        String link = loadaLinkFromDataBase("select link from LINKS_TO_BE_PROCESSED LIMIT 1");
        if (link == null) {
            return null;
        }
//        updateDatabase(link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
        return link;
    }

    @Override
    public void insertLinkToBeProcessed(String href) {

    }

    @Override
    public void insertProcessedLink(String link) {

    }

    //将处理过的link加入到 LINKS_ALREADY_PROCESSED 表中
//    @Override
//    public void updateDatabase(String href, String s) throws SQLException {
//        try (PreparedStatement ps = connection.prepareStatement(s)) {
//            ps.setString(1, href);
//            ps.executeUpdate();
//        }
//    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("insert into NEWS (URL,TITLE,CONTENT,CREATED_AT,MODIFIED_AT) values ( ?,?,?,now(),now() )")) {
            ps.setString(1, link);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement ps = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?")) {
            ps.setString(1, link);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                //处理过
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

}
