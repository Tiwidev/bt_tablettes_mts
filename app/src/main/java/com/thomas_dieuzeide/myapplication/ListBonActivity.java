package com.thomas_dieuzeide.myapplication;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ListBonActivity extends AppCompatActivity {
    private DatabaseHelper db;
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    private ArrayList<String> listItems=new ArrayList<String>();
    String[] infousr;

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    private ArrayAdapter<String> adapter;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MyApplication.getAppContext();
        db = new DatabaseHelper(context);
        setContentView(R.layout.activity_list_bon);
        ListView myList=(ListView)findViewById(R.id.listViewBon);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("LISTE DES BONS");
        actionBar.setDisplayOptions(actionBar.getDisplayOptions()
                | ActionBar.DISPLAY_SHOW_CUSTOM);
        ImageView imageView = new ImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.logo);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT
                | Gravity.CENTER_VERTICAL);
        layoutParams.rightMargin = 40;
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        myList.setAdapter(adapter);
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                final String displaykey = ((TextView) arg1).getText().toString();
                final String key = ((TextView) arg1).getText().toString().split(":")[0];
                final int session = getSessionNumber(key);
                if (session == 0) {
                    new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("BON: " + displaykey)
                            .setIcon(R.drawable.logo)
                            .setMessage("QUE FAIRE AVEC CE BON?")
                            .setPositiveButton("MODIFIER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent intent = new Intent(new Intent(ListBonActivity.this, MainActivity.class));
                                            Bundle b = new Bundle();
                                            b.putString("key", key); //Your id
                                            Cursor c = db.getSession(key, session);
                                            if (c.moveToNext()) {
                                                if (c.getString(3) != null) {
                                                    // previous session has been closed
                                                    java.util.Date dt = new java.util.Date();
                                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                    String currentTime = sdf.format(dt);
                                                    db.addSession(key, session + 1, currentTime);
                                                    ContentValues work = db.getWork(key, session);
                                                    db.addWork(key, work.get("demand").toString(), "", "", 1, work.get("devis").toString(), work.get("facture").toString(), session + 1);
                                                    b.putInt("session", session + 1);
                                                    intent.putExtras(b); //Put your id to your next Intent
                                                    ListBonActivity.this.startActivity(intent);
                                                } else {
                                                    b.putInt("session", session);
                                                    intent.putExtras(b); //Put your id to your next Intent
                                                    ListBonActivity.this.startActivity(intent);
                                                }
                                            } else {
                                                //open session 0 at first time we modify
                                                java.util.Date dt = new java.util.Date();
                                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                String currentTime = sdf.format(dt);
                                                db.addSession(key, 0, currentTime);
                                                intent.putExtras(b); //Put your id to your next Intent
                                                ListBonActivity.this.startActivity(intent);
                                            }
                                        }
                                    })
                            .setNeutralButton("CLOTURER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            Cursor c = db.getSession(key, session);
                                            if (c.moveToNext()) {
                                                if (c.getString(3) != null) {
                                                    // previous session has been closed
                                                    email(getApplicationContext(), "mtsbureau@gmail.com", key, "Informations sur " + displaykey + " en pièce jointe", getAttachments(key));
                                                    db.deleteBon(key);
                                                    listItems.remove(displaykey);
                                                    adapter.notifyDataSetChanged();
                                                    dialog.cancel();
                                                } else {
                                                    Toast.makeText(ListBonActivity.this, "LA DERNIERE SESSION EST PAS SIGNEE", Toast.LENGTH_LONG);
                                                }

                                            } else {
                                                Toast.makeText(ListBonActivity.this, "AUCUNE SESSION EXISTANTE", Toast.LENGTH_SHORT);
                                            }
                                        }
                                    })
                            .setNegativeButton("VISIONNER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent intent = new Intent(new Intent(ListBonActivity.this, MainActivity.class));
                                            Bundle b = new Bundle();
                                            b.putString("key", key); //Your id
                                            Cursor c = db.getSession(key, session);
                                            if (c.moveToNext()) {
                                                b.putInt("session", session);
                                                b.putInt("vision", 1);
                                                intent.putExtras(b); //Put your id to your next Intent
                                                ListBonActivity.this.startActivity(intent);
                                            } else {
                                                Toast.makeText(ListBonActivity.this, "IMPOSSIBLE DE VISIONNER UN BON VIDE", Toast.LENGTH_SHORT);
                                            }

                                        }
                                    })
                            .show();
                } else {
                    new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("BON: " + displaykey)
                            .setIcon(R.drawable.logo)
                            .setMessage("QUE FAIRE AVEC CE BON? NOMBRE DE MODIFICATIONS MAXIMUM ATTEINT!")
                            .setNegativeButton("VISIONNER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent intent = new Intent(new Intent(ListBonActivity.this, MainActivity.class));
                                            Bundle b = new Bundle();
                                            b.putString("key", key); //Your id
                                            Cursor c = db.getSession(key, session);
                                            if (c.moveToNext()) {
                                                b.putInt("session", session);
                                                b.putInt("vision", 1);
                                                intent.putExtras(b); //Put your id to your next Intent
                                                ListBonActivity.this.startActivity(intent);
                                            } else {
                                                Toast.makeText(ListBonActivity.this, "IMPOSSIBLE DE VISIONNER UN BON VIDE", Toast.LENGTH_SHORT);
                                            }

                                        }
                                    })
                            .setNeutralButton("CLOTURER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            Cursor c = db.getSession(key, session);
                                            if (c.moveToNext()) {
                                                if (c.getString(3) != null) {
                                                    // previous session has been closed
                                                    email(getApplicationContext(), "mtsbureau@gmail.com", key, "Informations sur " + displaykey + " en pièce jointe", getAttachments(key));
                                                    db.deleteBon(key);
                                                    listItems.remove(displaykey);
                                                    adapter.notifyDataSetChanged();
                                                    dialog.cancel();
                                                } else {
                                                    Toast.makeText(ListBonActivity.this, "LA DERNIERE SESSION EST PAS SIGNEE", Toast.LENGTH_LONG);
                                                }

                                            } else {
                                                Toast.makeText(ListBonActivity.this, "AUCUNE SESSION EXISTANTE", Toast.LENGTH_SHORT);
                                            }
                                        }
                                    })
                            .setNegativeButton("RETOUR",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();

                                        }
                                    })
                            .show();
                }
            }
        });

        infousr = getStringFromFile("infousr.txt");
        String old_name = db.getUsername().toUpperCase();
        if(old_name == "") {
            db.setUsername(Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase(),Integer.valueOf(infousr[2]));
        } else if(infousr[0].toUpperCase().charAt(0)!=old_name.charAt(0) || infousr[1].toUpperCase().charAt(0)!=old_name.charAt(1)) {
            db.updateUsername(old_name,Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase());
            newBonNumber(1);
        }

        // add new bon
        Button button = (Button) findViewById(R.id.addbon);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newid = getNewId();
                db.addBon(newid);
                listItems.add(newid);
                adapter.notifyDataSetChanged();
            }
        });

        // add new bon from excel
        Button button2 = (Button) findViewById(R.id.addbonexcel);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final NumberPicker numberPicker = new NumberPicker(ListBonActivity.this);
                List<String> titles = new ArrayList<String>();
                try {
                    titles = readExcelTitles();
                } catch(Exception e) {

                }
                final String[] excelTitles = titles.toArray(new String[titles.size()]);
                numberPicker.setMinValue(0);
                int max = Math.max(excelTitles.length - 1, 0);
                if (max == 0) {
                    AlertDialog bon_planning = new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("ATTENTION!")
                            .setIcon(R.drawable.logo)
                            .setMessage("TOUS LES BONS DISPONIBLES DANS LE PLANNING ONT ÉTÉ IMPORTÉS!")
                            .setNegativeButton("RETOUR",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .show();
                } else {
                    numberPicker.setMaxValue(max);
                    final String[] codes = getStringFromFile("codeclient.txt");
                    final String[] names = getStringFromFile("nomclient.txt");

                    numberPicker.setDisplayedValues(excelTitles);
                    numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    AlertDialog bon_planning = new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("CHOISISSEZ UN BON PROVENANT DU PLANNING: ")
                            .setIcon(R.drawable.logo)
                            .setNeutralButton("CONFIRMER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                String code = numberPicker.getDisplayedValues()[numberPicker.getValue()];
                                                List<String> data = readExcelData(code);
                                                String newid = getNewId();
                                                db.addBonCode(newid, code);
                                                String nomclient = "";
                                                if(Arrays.asList(codes).contains(data.get(7))) {
                                                    nomclient = names[Arrays.asList(codes).indexOf(data.get(7))];
                                                }
                                                db.addClient(data.get(7), nomclient, newid, data.get(8), false, data.get(10));
                                                db.addWork(newid, data.get(9), "", "",1,data.get(11),data.get(12),0);
                                                listItems.add(newid+ ": " + nomclient);
                                                adapter.notifyDataSetChanged();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                            .setNegativeButton("RETOUR",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .setView(numberPicker)
                            .show();
                }
            }
        });

        Cursor c = db.getAllBon();
        ContentValues c2;
        while (c.moveToNext()) {
            c2 = db.getClient(c.getString(0));
            if (c2.size() == 0) {
                listItems.add(c.getString(0));
            } else {
                listItems.add(c.getString(0) + ": " + c2.get("name_client"));
            }
        }
        adapter.notifyDataSetChanged();
    }

    public List<String> readExcelTitles() throws IOException {

        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file1 = new File(sdcard.getAbsolutePath());

        File inputWorkbook = new File(file1, Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase() + ".xls");

        if(! inputWorkbook.exists()){
            inputWorkbook = new File(file1, "GSK.xls");
        }
        List<String> resultSet = new ArrayList<String>();
        List<String> AlreadyDone = new ArrayList<String>();
        Cursor c = db.getAllBon();
        while (c.moveToNext()) {
            if(c.getString(2) != "None") {
                AlreadyDone.add(c.getString(2));
            }
        }

        if(inputWorkbook.exists()){
            Workbook w;
            try {
                w = Workbook.getWorkbook(inputWorkbook);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over column and lines
                for (int j = 1; j < sheet.getRows(); j++) {
                    Cell cell = sheet.getCell(0, j);
                    if (cell.getContents() != "" && !resultSet.contains(cell.getContents()) && !AlreadyDone.contains(cell.getContents())) {
                        resultSet.add(cell.getContents());
                    }
                }
            } catch (BiffException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            Toast.makeText(ListBonActivity.this, "PAS DE FICHIER EXCEL DISPONIBLE", Toast.LENGTH_LONG);
        }
        if(resultSet.size()==0){
            Toast.makeText(ListBonActivity.this, "LE FICHIER EXCEL EST VIDE", Toast.LENGTH_LONG);
        }
        return resultSet;
    }

    public List<String> readExcelData(String key) throws IOException  {
        List<String> resultSet = new ArrayList<String>();

        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file1 = new File(sdcard.getAbsolutePath());

        File inputWorkbook = new File(file1, Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase() + ".xls");

        if(! inputWorkbook.exists()){
            inputWorkbook = new File(file1, "GSK.xls");
        }
        if(inputWorkbook.exists()){
            Workbook w;
            try {
                w = Workbook.getWorkbook(inputWorkbook);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over column and lines
                for (int j = 1; j < sheet.getRows(); j++) {
                    Cell cell = sheet.getCell(0, j);
                    if(cell.getContents().equalsIgnoreCase(key)){
                        for (int i = 0; i < sheet.getColumns(); i++) {
                            Cell cel = sheet.getCell(i, j);
                            resultSet.add(cel.getContents());
                        }
                    }
                    continue;
                }
                System.out.println(Arrays.toString(resultSet.toArray()));
            } catch (BiffException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            Toast.makeText(ListBonActivity.this, "PAS DE FICHIER EXCEL DISPONIBLE", Toast.LENGTH_LONG);
        }
        if(resultSet.size()==0){
            Toast.makeText(ListBonActivity.this, "LE FICHIER EXCEL EST VIDE", Toast.LENGTH_LONG);
        }
        return resultSet;
    }

    @Override
    public void onRestart() {
        super.onRestart();
        listItems.clear();
        Cursor c = db.getAllBon();
        ContentValues c2;
        while (c.moveToNext()) {
            c2= db.getClient(c.getString(0));
            if(c2.size() == 0) {
                listItems.add(c.getString(0));
            } else {
                listItems.add(c.getString(0)+": "+c2.get("name_client"));
            }
        }
        adapter.notifyDataSetChanged();
    }

    public String getNewId() {
        String s =  db.getStatus();
        String[] status = s.split("-");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int year = cal.get(Calendar.YEAR);
        java.util.Date dt = new java.util.Date();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(dt);
        if(year == Integer.parseInt(status[1])){
            newBonNumber(Integer.valueOf(infousr[2]) + 1);
            infousr = getStringFromFile("infousr.txt");
            db.updateStatus(status[0],Integer.valueOf(infousr[2]),currentTime);

        } else {
            newBonNumber(1);
            infousr = getStringFromFile("infousr.txt");
            db.updateStatus(status[0],Integer.valueOf(infousr[2]),currentTime);
        }

        return s;
    }

    public int getSessionNumber(String key) {
        int result = 0;
        Cursor c = db.getAllSessions(key);
        while (c.moveToNext()) {
            int tmp = Integer.parseInt(c.getString(1));
            if( tmp > result){
                result = tmp;
            }
        }
        return result;
    }

    public void email(Context context, String emailTo,
                      String subject, String emailText, List<File> filePaths)
    {
        //need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> matches = pm.queryIntentActivities(emailIntent, 0);
        ResolveInfo best = null;
        for (final ResolveInfo info : matches)
            if (info.activityInfo.packageName.endsWith(".gm") ||
                    info.activityInfo.name.toLowerCase().contains("gmail")) best = info;
        if (best != null)
            emailIntent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{emailTo});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailText);

        //has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        //Compress comp = new Compress(filePaths.toArray(new File[filePaths.size()]),subject);
        // File zip = comp.zip();
        Uri u;

        //convert from paths to Android friendly Parcelable Uri's
        for (File file : filePaths)
        {
            u = Uri.fromFile(file);
            uris.add(u);
        }

        /*for (File file : filePaths)
        {
            file.delete();
        }*/



        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        Intent intent = Intent.createChooser(emailIntent, "Send mail...");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public List<File> getAttachments(String key) {
        final int session = getSessionNumber(key);
        final String k = key;
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/" + getResources().getString(R.string.external_dir) + "/");

        File[] res = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(":" + k + ":");
            }
        });

        List<File> r = Arrays.asList(res);
        r = new ArrayList<>(r);
        ContentValues client = db.getClient(key);
        ContentValues work;
        Cursor wkers;
        Cursor pices;
        Cursor sess;
        File nfo;
        String info = "<?xml version=\"1.0\"?>\n" +
                "<ss:Workbook xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n" +
                "    <ss:Styles>\n" +
                "        <ss:Style ss:ID=\"1\">\n" +
                "            <ss:Font ss:Bold=\"1\"/>\n" +
                "        </ss:Style>\n" +
                "    </ss:Styles>\n" +
                "    <ss:Worksheet ss:Name=\"Sessions\">\n" +
                "        <ss:Table>\n" +
                "            <ss:Column ss:Width=\"160\"/>\n" +
                "            <ss:Column ss:Width=\"80\"/>\n";

        for (int i = 0; i <= getSessionNumber(key);i++) {
            work = db.getWork(key, i);
            wkers = db.getAllWorkers(key, i);
            pices = db.getAllPieces(key, i);
            sess = db.getSession(key, i);
            info += infoText(client,work,sess,wkers,pices);
        }

        for (int i = 0; i <= 34 ; i++) {
            // fill form in order to delete eventual older second session info
            info += addRow("","");
        }

        info +=  "        </ss:Table>\n" +
                "    </ss:Worksheet>\n" +
                "</ss:Workbook>";

        nfo = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bons/",key+".xml");
        nfo.getParentFile().mkdirs();
        try{
            PrintWriter out = new PrintWriter(nfo.getAbsolutePath());
            out.println(info);
            out.close();
            r.add(nfo);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return r;
    }

    public String infoText(ContentValues cl,ContentValues w, Cursor sess, Cursor ws, Cursor pcs) {
        sess.moveToNext();
        String res = "";
        if(sess.getInt(1) == 0){
            res += addRow("Bon",cl.get("id").toString());
            res += addRow("Lieu de la prestation",cl.get("lieu").toString());
            res += addRow("Nom client",cl.get("name_client").toString());
            res += addRow("Code client", cl.get("code_client").toString());
            res += addRow("Nom Contact", cl.get("contact").toString());
            res += addRow("Travail demandé",w.get("demand").toString());
            res += addRow("Travail exécuté",w.get("execute").toString());
            res += addRow("Observations",w.get("observations").toString());
            res += addRow("Numéro Devis",w.get("devis").toString());
            res += addRow("Numéro Commande",w.get("facture").toString());
        } else {
            res += addRow("Travail exécuté",w.get("execute").toString());
            res += addRow("Observations",w.get("observations").toString());
        }

        int i = 0;
        while(pcs.moveToNext()) {
            res += addRow("Type de pièce et description", pcs.getString(1) + ":" + pcs.getString(2));
            res += addRowInt("Quantité",pcs.getString(3));
            i++;
        }

        for (int j = i; j < 9;j++) {
            res += addRow("Type de pièce et description","");
            res += addRow("Quantité","");
        }

        String tmp = sess.getString(2);
        res += addRow("Date de la prestation",tmp.substring(0, 10));
        res += addRowInt("Nombre de travailleurs", w.get("number").toString());
        res += addRow("Initiales",cl.get("id").toString().substring(0, 2));
        res += addRow("Début de la prestation", tmp.substring(11, 16));
        tmp = sess.getString(3);
        res += addRow("Fin de la prestation", tmp.substring(11, 16));
        if(ws.moveToNext()) {
            res += addRow("Initiales",ws.getString(1));
            res += addRow("Date",tmp.substring(0, 10));
            res += addRowInt("Quantité",ws.getString(5));
            res += addRow("Initiales",ws.getString(1));
            res += addRow("Arrivée",ws.getString(2));
            res += addRow("Départ",ws.getString(3));
        } else {
            res += addRow("Initiales","");
            res += addRow("Date","");
            res += addRowInt("Quantité","");
            res += addRow("Initiales","");
            res += addRow("Arrivée","");
            res += addRow("Départ","");
        }
        res += addRow("","");
        res += addRow("","");
        res += addRow("","");
        res += addRow("Signature", "");
        res += addRow("","");

        return res;
    }

    public String addRow(String s1,String s2) {
        String r = "            <ss:Row>\n" +
                "                <ss:Cell ss:StyleID=\"1\">\n" +
                "                   <ss:Data ss:Type=\"String\">"+s1+"</ss:Data>\n" +
                "                </ss:Cell>\n" +
                "                <ss:Cell>\n" +
                "                   <ss:Data ss:Type=\"String\">"+s2+"</ss:Data>\n" +
                "                </ss:Cell>\n" +
                "            </ss:Row>\n";

        return r;
    }

    public String addRowInt(String s1,String s2) {
        String r = "            <ss:Row>\n" +
                "                <ss:Cell ss:StyleID=\"1\">\n" +
                "                   <ss:Data ss:Type=\"String\">"+s1+"</ss:Data>\n" +
                "                </ss:Cell>\n" +
                "                <ss:Cell>\n" +
                "                   <ss:Data ss:Type=\"Number\">"+s2+"</ss:Data>\n" +
                "                </ss:Cell>\n" +
                "            </ss:Row>\n";

        return r;
    }

    public String[] getStringFromFile(String filename) {
        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file1 = new File(sdcard.getAbsolutePath());

        File file = new File(file1,filename);

        //Read text from file
        ArrayList<String> text = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file.getAbsolutePath()), "ISO-8859-1"));
            String line;

            while ((line = br.readLine()) != null) {
                text.add(line);
            }
            br.close();

        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            this.finish();
        }

        return text.toArray(new String[text.size()]);
    }

    public void newBonNumber(int n) {
        File f = new File(Environment.getExternalStorageDirectory(),"infousr.txt");
        try {
            FileWriter fw = new FileWriter(f.getAbsolutePath());
            String strs[] = { infousr[0], infousr[1], String.valueOf(n) };

            for (int i = 0; i < strs.length; i++) {
                fw.write(strs[i] + "\n");
            }
            fw.close();
        } catch(IOException e) {

        }

    }

}
