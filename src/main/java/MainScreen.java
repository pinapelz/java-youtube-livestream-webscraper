import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MainScreen {
    private JButton refreshButton;
    private JPanel MainPanel;
    private JTextPane textWindow;

    //Basic Measurements of time for conversion from milliseconds
    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    //Initalization of ArrayLists that will be used for storing the generated schedule
    static ArrayList<String> schedule;
    static ArrayList<String> finalschedule;
    Runnable scheduleRunner = new Runnable(){
        public void run(){
            int elapsed = 0;
            while(true) {
                if(elapsed>20){
                    refreshScreen();
                    elapsed = 0;
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                elapsed++;
            }
        }
    };
    Thread thread = new Thread(scheduleRunner);
    static HashMap<String, String> memberIDMap = fillHashMap("livers.txt"); //Add youtube channel and alias here in this file
    public MainScreen() {
        schedule= new ArrayList<String>();
        finalschedule = new ArrayList<String>();
        thread.start();
        refreshButton.addActionListener(new ActionListener() {//Refresh button at the bottom of the page to refresh data
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshScreen();
            }
        });
    }

    public static void main(String[] args) {
        JScrollPane scrollBar=new JScrollPane(new MainScreen().MainPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBar.getVerticalScrollBar().setUnitIncrement(10);
        JFrame frame = new JFrame("MainScreen");

        frame.setContentPane(scrollBar);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width/2, screenSize.height/2);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        frame.setVisible(true);
    }
    public String checkChannelLive(String name,String channelUrl) throws IOException {
            String title = "Currently Not Streaming";
            String unixTime = "LIVE RIGHT NOW";
            String timeUntil = "LIVE RIGHT NOW";
            String html = Jsoup.connect(channelUrl).get().html();

            Document doc = Jsoup.parse(html);
            Elements scriptElements = doc.getElementsByTag("script");
            DataNode youtubeVariables = null;
            for (Element element : scriptElements) {
                for (DataNode node : element.dataNodes()) {
                    if (element.data().contains("var ytInitialPlayerResponse")) {
                        youtubeVariables = node;

                    }
                }
            }
            try {
                if (youtubeVariables.equals(null)) {
                }
            } catch (Exception e) {

            }
            try {
                Pattern pattern = Pattern.compile("\"scheduledStartTime\":\"(.*?)\"");
                Matcher matcher = pattern.matcher(youtubeVariables.toString());
                Pattern titlePattern = Pattern.compile(",\"title\":\"(.*?)\"");
                Matcher titleMatcher = titlePattern.matcher(youtubeVariables.toString());
                titleMatcher.find();
                if (matcher.find()) {
                    unixTime = matcher.group(1);
                }
                title = titleMatcher.group(1);
                Date date = new java.util.Date(Integer.parseInt(unixTime) * 1000L);
                Date currentDate = new Date();
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                long days = -1;
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
                long difference_In_Time = date.getTime() - currentDate.getTime();
                StringBuffer text = new StringBuffer("");
                if (difference_In_Time > DAY) {
                    days = difference_In_Time/DAY;
                    text.append(difference_In_Time / DAY).append(" days ");
                    difference_In_Time %= DAY;
                }
                if (difference_In_Time > HOUR) {
                    text.append(difference_In_Time / HOUR).append(" hours ");
                    difference_In_Time %= HOUR;
                }
                if (difference_In_Time > MINUTE) {
                    text.append(difference_In_Time / MINUTE).append(" minutes ");
                    difference_In_Time %= MINUTE;
                }
                if (difference_In_Time > SECOND) {
                    text.append(difference_In_Time / SECOND).append(" seconds ");
                    difference_In_Time %= SECOND;
                }
                if(days==-1||days<3){
                    timeUntil = text.toString();
                }
                else{
                    timeUntil = "FREECHATEXCEPTION";
                }





            } catch (Exception e) {


            }

        return "<strong style=\"font-size:20px\">"+name+":&nbsp;</strong>"+ "<span style=\"font-size:16px\">live in "+ timeUntil+"<span>      <br style=\"font-size:13px\">Activity:"+title+"</br><hr>";
        }
        public void refreshScreen(){

            textWindow.setText(null);
            //Clearing both arraylists to make sure they are empty for the rerfresh
            finalschedule.clear();
            schedule.clear();
            PrintWriter writer = null;
            try {
                writer = new PrintWriter("HTMLData.txt");
                writer.print("");
                writer.close();
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }

            Set<String> keySet = memberIDMap.keySet();
            ArrayList<String> listOfKeys = new ArrayList<String>(keySet); //Converting keys (names) into arraylist so its iterable

            for(int i = 0;i<listOfKeys.size();i++){
                try {
                    schedule.add(checkChannelLive(listOfKeys.get(i),"https://www.youtube.com/channel/"+memberIDMap.get(listOfKeys.get(i))+"/live"));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            for(int i=0;i<listOfKeys.size();i++){
                if(schedule.get(i).contains("Activity:Currently Not Streaming")||schedule.get(i).contains("FREECHATEXCEPTION")){

                }
                else{
                    finalschedule.add(schedule.get(i));
                }
            }
            for(int i =0;i<finalschedule.size();i++){
                try {
                    Files.write(Paths.get("HTMLData.txt"),(finalschedule.get(i)+" ").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }

            Path fileName = Path.of("HTMLData.txt");
            try {
                textWindow.setContentType("text/html");
                textWindow.setText(Files.readString(fileName));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }


        }
    public static HashMap<String, String> fillHashMap(String file){
        String delimiter = ":";
        HashMap<String, String> map = new HashMap<>();
        try(Stream<String> lines = Files.lines(Paths.get(file))){
            lines.filter(line -> line.contains(delimiter)).forEach(line ->
                    map.putIfAbsent(line.split(delimiter)[0]
                            , line.split(delimiter)[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
    }
