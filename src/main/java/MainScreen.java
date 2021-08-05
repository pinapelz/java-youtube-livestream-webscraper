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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MainScreen {
    private JButton refreshButton;
    private JPanel MainPanel;
    private JTextPane textWindow;
    private int refreshTimer = 500;//How often do you want to autorefresh the screen
    //Basic Measurements of time for conversion from milliseconds
    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    //Initalization of ArrayLists that will be used for storing the generated schedule
    static ArrayList<String> schedule;
    static ArrayList<String> finalschedule;
    static ArrayList<Integer> unixList;
    static ArrayList<String> tempsched = new ArrayList<String>(); //Temporary Arraylist used to swap data between 2 arraylists
    static ArrayList<String> currentlyLive = new ArrayList<String>();

    Runnable scheduleRunner = new Runnable(){//Creating a new runnable thread to autorefresh the data
        public void run(){
            int elapsed = 0; //Elapsed time in seconds
            while(true) {
                if(elapsed>refreshTimer){ //Once elapsed time has exceeded the set timer variable
                    refreshScreen(); //Refresh the data shown on the JPanel
                    elapsed = 0;
                }
                try {
                    TimeUnit.SECONDS.sleep(1); //Wait 1 second
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
        schedule= new ArrayList<String>(); //Initializing Arraylists
        unixList = new ArrayList<Integer>();
        finalschedule = new ArrayList<String>();
        thread.start();
        refreshButton.addActionListener(new ActionListener() {//Refresh button at the bottom of the page to refresh data
            //Action Listener code below checking for button click
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshScreen(); //Runs on button click
            }
        });
    }

    public static void main(String[] args) {
        //Scroll pane adds a scroll bar to the side
        JScrollPane scrollBar=new JScrollPane(new MainScreen().MainPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBar.getVerticalScrollBar().setUnitIncrement(10);  //Make it scroll faster

        JFrame frame = new JFrame("Schedule");

        frame.setContentPane(scrollBar);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width/2, screenSize.height/2);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        frame.setVisible(true);
    }
    //Grabs the data of a youtube channel to see whether they are live or not or have upcoming streams
    public String checkChannelLive(String name,String channelUrl) throws IOException {
        //Default variable initialization below
            String title = "Currently Not Streaming";
            String unixTime = "0";
            String streamDate = "NA";
            String timeUntil = "0";

            //Using JSoup we grab the entire html file of the site
            String html = Jsoup.connect(channelUrl).get().html();
            Document doc = Jsoup.parse(html); //Put it all into a document so its easier to work with

        //Search the entire document for elements with the "script" tag.
        //Add them all into Elements (basically an ArrayList<Element>)
            Elements scriptElements = doc.getElementsByTag("script");
            DataNode youtubeVariables = null; //DataNode is used to store an indiviudal script

        //Iterate through all of the scripts that we retreived from the HTML
            for (Element element : scriptElements) {
                for (DataNode node : element.dataNodes()) {
                    //Looking for an script element that is called "var ytInitialPlayerResponse"
                    if (element.data().contains("var ytInitialPlayerResponse")) {
                        youtubeVariables = node; //Located! So let's set the node as this element

                    }
                }
            }
            //I don't know how you would even end up in this situation so long as you feed the code a YouTube channel URL
            //However I guess if you do, then just end the program
            try {
                if (youtubeVariables.equals(null)) {
                    System.exit(0);
                }
            } catch (Exception e) {

            }
            try {
                //Using regex we can parse specific information about the stream
                Pattern pattern = Pattern.compile("\"scheduledStartTime\":\"(.*?)\""); //Scheduled start time (in UNIX)
                Matcher matcher = pattern.matcher(youtubeVariables.toString());
                Pattern titlePattern = Pattern.compile(",\"title\":\"(.*?)\"");//Title of the upcoming stream
                Matcher titleMatcher = titlePattern.matcher(youtubeVariables.toString());

                //Searching throught the string for the regex we've specified and taking the first occurance
                titleMatcher.find();
                if (matcher.find()) {
                    unixTime = matcher.group(1);
                }
                title = titleMatcher.group(1);

                //Convert the UNIX time integer into a date
                Date date = new java.util.Date(Integer.parseInt(unixTime) * 1000L);
                Date currentDate = new Date();
                //Formatting the date into a readable format
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                long days = -1;
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
                streamDate = sdf.format(date);

                //Getting the difference in time (how long until the stream)
                long difference_In_Time = date.getTime() - currentDate.getTime();
                StringBuffer text = new StringBuffer("");

                //Converting milliseconds into a readable format
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

                //If the stream is more than 3 days away don't show it
                if(days==-1||days<3){
                    timeUntil = text.toString();
                }
                else{

                    timeUntil = "FREECHATEXCEPTION";
                }





            } catch (Exception e) {


            }
        //Return the newly made HTML String
        return "<strong style=\"font-size:20px\">"+name+":&nbsp;</strong>"+ "<span style=\"font-size:16px\">live in "+ timeUntil+"<span>      <br style=\"font-size:13px\">Activity:"+title+"</br><br>"+streamDate+"  ("+unixTime+")<hr>";
        }
        public void refreshScreen(){ //This function puts the data onto the screen

            textWindow.setText(null);
            //Clearing all arraylists so previous data is gone
            finalschedule.clear();
            currentlyLive.clear();
            tempsched.clear();
            unixList.clear();
            schedule.clear();
            PrintWriter writer = null;
            //Clearing the HTMLData text file
            try {
                writer = new PrintWriter("HTMLData.txt");
                writer.print("");
                writer.close();
            } catch (FileNotFoundException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }

            Set<String> keySet = memberIDMap.keySet();
            ArrayList<String> listOfKeys = new ArrayList<String>(keySet); //Converting keys (names) into arraylist so its iterable

            //Fill an arraylist with the HTML Data for each channel
            for(int i = 0;i<listOfKeys.size();i++){
                try {
                    schedule.add(checkChannelLive(listOfKeys.get(i),"https://www.youtube.com/channel/"+memberIDMap.get(listOfKeys.get(i))+"/live"));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            for(int i=0;i<listOfKeys.size();i++){
                //Remove people who aren't streaming or have a stream that's too far away
                if(schedule.get(i).contains("Activity:Currently Not Streaming")||schedule.get(i).contains("FREECHATEXCEPTION")){

                }
                else{
                    finalschedule.add(schedule.get(i));
                }
            }

            //Check for the unix time which we wrote into the HTML text for each channel
            for(int i = 0;i<finalschedule.size();i++){
                //We are searching for the unix time sort the list of streamers by difference in time (so we know who's streaming first)
                Pattern pattern = Pattern.compile("^.*?\\([^\\d]*(\\d+)[^\\d]*\\).*$");
                Matcher matcher = pattern.matcher(finalschedule.get(i));
                matcher.find();
                if(matcher.group(1).equals("0")){
                    currentlyLive.add(finalschedule.get(i)); //If we wrote the unix time as 0 then that means they are live
                    //There will be no number between the parentheses if the streamer is not live
                }
                else {
                    //Add all the unix times into another arraylist
                    unixList.add(Integer.parseInt(matcher.group(1)));
                }
            }
            Collections.sort(unixList); //Sort that arraylist in order

            //We iterate through the list of UNIX Times that have already been sorted
            //We assign the schedule into a temporary arraylist in order

           for(int i = 0;i<unixList.size();i++){
                boolean flag = false;
                int j = 0;
                while(flag==false){
                    if(finalschedule.get(j).contains(Integer.toString(unixList.get(i)))){ //Checking to see if current unix time index is in the finalschedule index
                        tempsched.add(finalschedule.get(j)); //If it is then add it to the temporary arraylist

                        finalschedule.remove(j);
                        flag = true;
                    }
                    j++;
                }
            }
           //By the end of this loop we will get a new arraylist of the HTML Data of each channel sorted in order
            finalschedule.clear();
           //In these two for loops we prepare the finalschedule arraylist for export
            for(int i =0;i< currentlyLive.size();i++){
                finalschedule.add(currentlyLive.get(i).replaceAll("live in","CURRENTLY LIVE!")); //Add the currently live channels first
            }
            for(int i = 0;i<tempsched.size();i++){
                finalschedule.add(tempsched.get(i)); //Add the upcoming schedules in one by one
            }




            for(int i =0;i<finalschedule.size();i++){ //Finally we write the arraylist of html into a text file
                try {
                    Files.write(Paths.get("HTMLData.txt"),(finalschedule.get(i)+" ").getBytes(), StandardOpenOption.APPEND);
                    System.out.println("Writing: " + (finalschedule.get(i)+" "));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }

            Path fileName = Path.of("HTMLData.txt"); //Then we take the text inside the text file and we print it onto te screen
            try {
                textWindow.setContentType("text/html");
                textWindow.setText(Files.readString(fileName));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }


        }
    public static HashMap<String, String> fillHashMap(String file){ //Fill hashmap from a text file
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
