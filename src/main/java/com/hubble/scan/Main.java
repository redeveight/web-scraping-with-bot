package com.hubble.scan;

import com.hubble.scan.utls.MinecraftServerStatus;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hubble.scan.utls.ServersInfo.*;

public class Main {
    public static final String ACCESS_TOKEN = "###";
    public static final String CHANNEL_TAG = "minecrafthubble";
    private static final int TIMEOUT = 10000;
    private static final String CURRENT_DIR = System.getProperty("user.dir");
    private static final String FILE_NAME = "servers_payments.txt";

    private static Connection connection;
    private static Statement statement;

    public static void main(String... args) {
        try {
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.MINUTES);
        }
        catch (Exception e) {
            System.out.println("main -" + e.toString());
        }
    }

    private static Runnable runnable = new Runnable() {
        public void run() {
            try {
                SimpleDateFormat formatter= new SimpleDateFormat("HH:mm z");
                Date date = new Date(System.currentTimeMillis());
                System.out.printf("%s - Scraping...%n", formatter.format(date));
                date = null; formatter = null;
                if (scrapPayments()) save();
            } catch (Exception e) {
                System.out.println("runnable -" + e.toString());
            }
        }
    };

    private static boolean scrapPayments() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", "py scraping.py");
            Process process = processBuilder.start();

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                process.destroy();
                processBuilder = null;
                process = null;
                System.out.println("Scraping completed. Writing to DB...");
                return true;
            }
        } catch (Exception e) {
            System.out.println("scrapPayments -" + e.toString());
        }
        return false;
    }

    private static String[] linesFile;

    private static void save() {
        try {
            linesFile = getLinesFromFile().split("\n");
            for (int i = 0; i < getCountServers(); i++) {
                for (int j = 0; j < 5; j++) {
                    Payment payment = getPayment(i, j);
                    if (payment != null) writeToDB(getServers()[i].getName(), payment);
                }
            }
            System.out.println("Writing completed.");
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (Exception e) {
                System.out.println("runnable-cls -" + e.toString());
            }
            connect();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            String dateString = format.format(new Date());
            float paymentValue = getTodayPaymentsValue();
            float minusNineProcent = paymentValue - (paymentValue * 9 / 100);
            float minus29Procent = paymentValue - (paymentValue * 29 / 100);
            System.out.printf("[%s] Today's revenue: %s/%s/%s%n",
                    dateString,
                    String.format("%.0f", minus29Procent),
                    String.format("%.0f", minusNineProcent),
                    String.format("%.0f", paymentValue)
            );
            System.out.println("\n" + getTodayServersPaymentsString());
            disconnect();
        } catch (Exception e) {
            System.out.println("save -" + e.toString());
        }
    }

    private static String getLinesFromFile() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("\\" + FILE_NAME), "UTF8"));
            String lines = "", line;
            line = reader.readLine();
            while (line != null) {
                lines += (line + "\n");
                line = reader.readLine();
            }
            reader.close();
            return lines;
        } catch (Exception e) {
            System.out.println("getLinesFromFile -" + e.toString());
        }
        return null;
    }

    private static Payment getPayment(int i, int j) {
        try {
            if (!linesFile[i].isEmpty()) {
                Document document = Jsoup.parse(linesFile[i]);
                String title = document.getElementsByClass("payment-id window item-id").get(j).getElementsByAttribute("data-id").first().getElementsByClass("title").text();
                String player = document.getElementsByClass("payment-id window item-id").get(j).getElementsByAttribute("data-id").first().getElementsByClass("player").text().replace("─ ", "");
                float price = Float.parseFloat(document.getElementsByClass("payment-id window item-id").get(j).getElementsByAttribute("data-id").first().getElementsByClass("price").text().replace(" ", ""));
                int amount = 1;
                try {
                    amount = Integer.parseInt(document.getElementsByClass("payment-id window item-id").get(j).getElementsByAttribute("data-id").first().getElementsByClass("amount").text().replace("x", ""));
                } catch (Exception e) {}
                String firstLine = document.getElementsByClass("payment-id window item-id").get(j).getElementsByAttribute("data-id").first().toString().split("\n")[0];
                String dataId = firstLine = firstLine.substring(firstLine.indexOf("data-id=\"")).replace("data-id=\"", "");
                dataId = firstLine.substring(0, firstLine.indexOf("\""));
                return new Payment(Integer.parseInt(dataId), title, player, price, amount);
            }
        } catch (Exception e) {
            System.out.println("getPayment -" + e.toString());
        }
        return null;
    }

    private static void writeToDB(String serverName, Payment payment) {
        try {
            connect();
            if (!isAlreadyExist(serverName, payment)) {
                PreparedStatement preparedStatement = null;
                String query = "INSERT INTO payments VALUES(null, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                connection.setAutoCommit(false);
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, serverName);
                preparedStatement.setInt(2, payment.getDataId());
                preparedStatement.setString(3, payment.title);
                preparedStatement.setString(4, payment.player);
                preparedStatement.setFloat(5, payment.price);
                preparedStatement.setInt(6, payment.amount);
                preparedStatement.executeUpdate();
                connection.commit();
                preparedStatement.close();
            }
            disconnect();
        }
        catch(SQLException e) {
            System.err.println("writeToDB -" + e.toString());
        }
    }

    public static void connect() {
        try {
            String url = "jdbc:sqlite:payments.db";
            connection = DriverManager.getConnection(url);

            statement = connection.createStatement();
            statement.setQueryTimeout(50);

            /*statement.executeUpdate("drop table if exists payments");
            statement.executeUpdate("create table payments (id integer primary key autoincrement, server_name string, data_id integer, title string, player string, price real, amount integer, scan_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            System.exit(0);*/
        } catch (SQLException e) {
            System.out.println("connect -" + e.toString());
        }
    }

    private static boolean isAlreadyExist(String serverName, Payment payment) {
        try {
            if (payment != null && payment.player != null) {
                //String query = "SELECT id FROM payments WHERE (scan_time >= datetime('now','-2 day','localtime') AND server_name=? AND data_id=?);";
                String query = "SELECT id FROM payments WHERE server_name=? AND data_id=?;";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, serverName);
                preparedStatement.setInt(2, payment.dataId);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet != null && resultSet.next()) {
                    resultSet.close();
                    preparedStatement.clearParameters();
                    preparedStatement.close();
                    return true;
                }
            } else return true;
        } catch (Exception e) {
            System.out.println("isAlreadyExist -" + e.toString());
        }
        return false;
    }

    private static float getTodayPaymentsValue() {
        try {
            DateTime dt = new DateTime();
            int minutes = (dt.getHourOfDay() * 60) + dt.getMinuteOfHour();
            String query = "SELECT SUM(price*amount) FROM payments WHERE scan_time >= datetime('now','-! minute','localtime');".replace("!", String.valueOf(minutes));
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet != null && resultSet.next()) {
                float value = resultSet.getFloat(1);
                resultSet.close();
                preparedStatement.clearParameters();
                preparedStatement.close();
                preparedStatement = null;
                resultSet = null;
                dt = null;
                return value;
            }
        } catch (Exception e) {
            System.out.println("getTodayPaymentsValue -" + e.toString());
        }
        return 0;
    }

    private static String getTodayServersPaymentsString() {
        try {
            String result = "";
            DateTime dt = new DateTime();
            int minutes = (dt.getHourOfDay() * 60) + dt.getMinuteOfHour();
            for (int i = 0; i < getCountServers(); i++) {
                String query = "SELECT SUM(price*amount) FROM payments WHERE scan_time >= datetime('now','-! minute','localtime') AND server_name=?;".replace("!", String.valueOf(minutes));
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, getServers()[i].getName());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet != null && resultSet.next()) {
                    float value = resultSet.getFloat(1);
                    if (getServers()[i].getName().equals("musteryworld.ru")) value = value / 2;
                    float minusNineProcent = value - (value * 9 / 100);
                    float minus29Procent = value - (value * 29 / 100);
                    result += (getServers()[i].getName() + ": "
                            + String.format("%.0f", minus29Procent)
                            + "/" + String.format("%.0f", minusNineProcent) + "/" + String.format("%.0f", value) + "\n");
                    resultSet.close();
                    preparedStatement.clearParameters();
                    preparedStatement.close();
                    preparedStatement = null;
                    resultSet = null;
                    dt = null;
                }
            }
            return result;
        } catch (Exception e) {
            System.out.println("getTodayPaymentsValue -" + e.toString());
        }
        return null;
    }

    public static void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("disconnect -" + e.toString());
        }
    }

    private static class Payment {
        private int dataId;
        private String title;
        private String player;
        private float price;
        private int amount;

        public Payment(int dataId, String title, String player, float price, int amount) {
            this.dataId = dataId;
            this.title = title;
            this.player = player;
            this.price = price;
            this.amount = amount;
        }

        public int getDataId() {
            return dataId;
        }

        public String getTitle() {
            return title;
        }

        public String getPlayer() {
            return player;
        }

        public float getPrice() {
            return price;
        }

        public int getAmount() {
            return amount;
        }
    }
}