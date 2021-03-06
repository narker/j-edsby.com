package com.cheenar.jedsby.packets;

import com.cheenar.jedsby.JEdsby;
import com.cheenar.jedsby.data.DataLogin;
import com.cheenar.jedsby.data.DataStudentClass;
import com.cheenar.jedsby.resources.Student;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;

/**
 * Created by admin on 3/26/16.
 */
public class PacketAcquireClasses extends Packet
{

    private Student student;
    private ArrayList<DataStudentClass> studentClasses;
    private DataLogin loginData;

    public PacketAcquireClasses(String cook, DataLogin loginData)
    {
        super("https://" + JEdsby.HOST_NAME() + ".edsby.com/core/node.json/" + loginData.getUniqueStudentIdentifier() + "?xds=BaseStudent", Packet.ERequestMethod.GET);
        setCookies(cook);

        this.loginData = loginData;
        studentClasses = new ArrayList<>();
    }

    @Override
    public void execute()
    {
        super.execute();
        setScheme("https");
        setAccept("*/*");
        setAcceptEncoding("gzip, deflate, sdch");
        setAcceptLanguage("en-US,en;q=0.8,es;q=0.6");
        setCookies(getCookies());
        setUserAgent(JEdsby.USER_AGENT());
        try
        {
            sendPacket();
            parseClasses();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void parseClasses() throws Exception
    {
        StringBuilder sb = getDataFromGZIP();

        JsonObject asd = (JsonObject) new JsonParser().parse(sb.toString());
        JsonObject map = (JsonObject) asd.getAsJsonArray("slices").get(0);
        JsonObject data = (JsonObject) map.get("data");
        JsonObject col1 = (JsonObject) data.get("col1");
        JsonObject classes = (JsonObject) col1.get("classes");
        JsonObject classContainer = (JsonObject) classes.get("classesContainer");
        JsonObject clazzes = (JsonObject) classContainer.get("classes");

        System.out.println(clazzes.toString());

        Object o = new Gson().fromJson(clazzes, Object.class);
        LinkedTreeMap l = (LinkedTreeMap) o;
        ArrayList<String> classRids = new ArrayList<>();
        for(Object s : l.keySet())
        {
            classRids.add(s.toString());
        }

        studentClasses = new ArrayList<>();
        for(int k = 0; k < classRids.size(); k++)
        {
            DataStudentClass sC = new DataStudentClass(clazzes.get(classRids.get(k)).toString());
            studentClasses.add(sC);
        }

        student = new Student(loginData.getName(), loginData.getUniqueStudentIdentifier(), studentClasses);

        for(DataStudentClass sc : student.getClasses())
        {
            resolveGrade(sc);
        }
    }

    private void resolveGrade(DataStudentClass sC) throws Exception
    {
        PacketGatherGrades packet = new PacketGatherGrades(getCookies(), student, sC);
        packet.execute();

        StringBuilder sb = packet.getDataFromGZIP();

        JsonObject objs = (JsonObject) new JsonParser().parse(sb.toString());
        JsonArray slices = objs.getAsJsonArray("slices");
        JsonObject data = (JsonObject)slices.get(0);
        JsonObject sliceData = (JsonObject) data.get("data");
        JsonObject loaddata = (JsonObject)sliceData.get("loaddata");

        sC.setGrade(new String(String.valueOf(loaddata.get("average"))));
    }

    public Student getStudent()
    {
        return student;
    }

    public ArrayList<DataStudentClass> getStudentClasses()
    {
        return studentClasses;
    }
}
