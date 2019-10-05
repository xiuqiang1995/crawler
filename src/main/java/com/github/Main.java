package com.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/br/githubws/crawler-1/crawlerDB", "root", "root");
        String link;
        //从数据库加载一个待处理link
        while ((link = getOneLinkAndDeleteIt(connection)) != null) {
            //断点续传
            if (!isLinkProcessed(connection, link)) {
                if (isNewsLink(link)) {
//                    System.out.println("link = " + link);
                    Document doc = HttpGetAndParseHtml(link);
                    parseLinkAndStoreIntoDataBase(connection, doc);
                    storeIntoDataBase(connection, doc, link);
                    updateDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values (?)");
                }
            }

        }
    }

    private static String getOneLinkAndDeleteIt(Connection connection) throws SQLException {
        //待处理链接池
        String link = loadALinksFromDataBase(connection, "select link from LINKS_TO_BE_PROCESSED LIMIT 1");
        if (link == null) {
            return null;
        }
        updateDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
        return link;
    }

    private static void parseLinkAndStoreIntoDataBase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
                System.out.println("href2 = " + href);
            }
            //将爬到的LINK存入 LINKS_TO_BE_PROCESSED 表中
            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (link) values (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    //将处理过的link加入到 LINKS_ALREADY_PROCESSED 表中
    private static void updateDatabase(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(s)) {
            ps.setString(1, href);
            ps.executeUpdate();
        }
    }

    private static String loadALinksFromDataBase(Connection connection, String sql) throws SQLException {
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

    private static void storeIntoDataBase(Connection conn, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            System.out.println("title = " + title);
            String content = articleTags.get(0).select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
            try (PreparedStatement ps = conn.prepareStatement("insert into NEWS (URL,TITLE,CONTENT,CREATED_AT,MODIFIED_AT) values ( ?,?,?,now(),now() )")) {
                ps.setString(1, link);
                ps.setString(2, title);
                ps.setString(3, content);
                ps.executeUpdate();
            }

        }
    }

    private static Document HttpGetAndParseHtml(String link) throws IOException {
//        System.out.println("link = " + link);
        //从官方文档抄代码 http://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println("href1 ------------------ = " + link);
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        Document doc = null;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            doc = Jsoup.parse(html);

        }
        return doc;
    }

    private static boolean isNewsLink(String link) {
        //is Index Page ?
        if ("https://sina.cn".equals(link)) {
            return true;
        }
        //is news page and is not login page
        return link.contains("news.sina.cn") && !link.contains("passport.sina.cn");
    }
}
