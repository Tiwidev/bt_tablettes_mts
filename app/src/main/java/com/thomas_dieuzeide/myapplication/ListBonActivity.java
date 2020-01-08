package com.thomas_dieuzeide.myapplication;

import android.annotation.TargetApi;
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
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.EditText;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
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
    @TargetApi(11)
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
                final Cursor c = db.getSession(key, session);
                final boolean existingSession = c.moveToNext();
                final boolean signedSession = existingSession && c.getString(3) != null;
                if (session == 0 || (session == 1 && ! signedSession)) {
                    String resume = "\n\n";
                    if (! existingSession) {
                        resume += "PAS DE SESSION OUVERTE!";
                    } else if (session == 0) {
                        if (signedSession) {
                            resume += "PREMIERE SESSION SIGNEE! MODIFIER OUVRIRA LA DEUXIEME SESSION.";
                        } else {
                            resume += "PREMIERE SESSION OUVERTE! SIGNEZ QUAND LE TRAVAIL EST FINI.";
                        }
                    } else {
                        resume += "DEUXIÈME SESSION OUVERTE! SIGNEZ QUAND LE TRAVAIL EST FINI.";
                    }
                    new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("BON: " + displaykey)
                            .setIcon(R.drawable.logo)
                            .setMessage("QUE FAIRE AVEC CE BON?" + resume)
                            .setPositiveButton("MODIFIER",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent intent = new Intent(new Intent(ListBonActivity.this, MainActivity.class));
                                            Bundle b = new Bundle();
                                            b.putString("key", key); //Your id
                                            if (existingSession) {
                                                if (signedSession) {
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
                                            if (existingSession) {
                                                if (signedSession) {
                                                    // previous session has been closed
                                                    cloture(db,listItems,adapter,dialog,key,displaykey);
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
                                            if (existingSession) {
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
                                            if (existingSession) {
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
                                            if (existingSession) {
                                                if (signedSession) {
                                                    // previous session has been closed
                                                    cloture(db,listItems,adapter,dialog,key,displaykey);
                                                } else {
                                                    Toast.makeText(ListBonActivity.this, "LA DERNIERE SESSION EST PAS SIGNEE", Toast.LENGTH_LONG);
                                                }

                                            } else {
                                                Toast.makeText(ListBonActivity.this, "AUCUNE SESSION EXISTANTE", Toast.LENGTH_SHORT);
                                            }
                                        }
                                    })
                            .setPositiveButton("RETOUR",
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
                if (! findExcelFile()) {
                    AlertDialog bon_planning = new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("ATTENTION!")
                            .setIcon(R.drawable.logo)
                            .setMessage("PAS DE PLANNING TROUVÉ!")
                            .setNegativeButton("RETOUR",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .show();
                } else {
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
                                                    String nomclient = data.get(7);
                                                    if(Arrays.asList(codes).contains(data.get(7))) {
                                                        nomclient = names[Arrays.asList(codes).indexOf(data.get(7))];
                                                    }
                                                    if (nomclient == data.get(7)) {
                                                        db.addClient("999999", nomclient, newid, data.get(8), true, data.get(10));
                                                    } else {
                                                        db.addClient(data.get(7).replaceAll("\\s+",""), nomclient, newid, data.get(8), false, data.get(10));
                                                    }

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

    public boolean findExcelFile() {
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding("Cp1252");
        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        //Get the text file
        File file1 = new File(sdcard.getAbsolutePath());

        File inputWorkbook = new File(file1, Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase() + ".xls");

        if(! inputWorkbook.exists()){
            inputWorkbook = new File(file1, "GSK.xls");
        }
        return inputWorkbook.exists();
    }

    public List<String> readExcelTitles() throws IOException {
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding("Cp1252");
        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

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
                w = Workbook.getWorkbook(inputWorkbook, ws);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over column and lines
                for (int j = 1; j < sheet.getRows(); j++) {
                    Cell cell = sheet.getCell(0, j);
                    if (cell.getContents() != "" && !resultSet.contains(cell.getContents()) && !AlreadyDone.contains(cell.getContents())) {
                        resultSet.add(cell.getContents().replaceAll("'", "''"));
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
        WorkbookSettings ws = new WorkbookSettings();
        ws.setEncoding("Cp1252");
        List<String> resultSet = new ArrayList<String>();

        //Find the directory for the SD Card using the API
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        //Get the text file
        File file1 = new File(sdcard.getAbsolutePath());

        File inputWorkbook = new File(file1, Character.toString(infousr[0].charAt(0)).toUpperCase() + Character.toString(infousr[1].charAt(0)).toUpperCase() + ".xls");

        if(! inputWorkbook.exists()){
            inputWorkbook = new File(file1, "GSK.xls");
        }
        if(inputWorkbook.exists()){
            Workbook w;
            try {
                w = Workbook.getWorkbook(inputWorkbook, ws);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over column and lines
                for (int j = 1; j < sheet.getRows(); j++) {
                    Cell cell = sheet.getCell(0, j);
                    if(cell.getContents().equalsIgnoreCase(key)){
                        for (int i = 0; i < sheet.getColumns(); i++) {
                            Cell cel = sheet.getCell(i, j);
                            resultSet.add(cel.getContents().replaceAll("'", "''"));
                        }
                    }
                    continue;
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

    public void cloture(DatabaseHelper d, ArrayList<String> listI, ArrayAdapter<String> ad,DialogInterface dialog, String k, String displayk) {
        final DatabaseHelper db = d;
        final String key = k;
        final String displaykey = displayk;
        final ArrayList<String> listItems = listI;
        final ArrayAdapter<String> adapter = ad;
        final EditText emailEditText = new EditText(ListBonActivity.this);
        ContentValues client = db.getClient(key);
        emailEditText.setHint("METTRE ADRESSE EMAIL DU CLIENT ICI POUR LEUR ENVOYER LE BON");
        AlertDialog bon_planning = new AlertDialog.Builder(ListBonActivity.this)
                            .setTitle("SOUHAITEZ-VOUS ENVOYEZ LES INFORMATIONS DE CE BON AU CLIENT ET/OU A MTS?")
                            .setIcon(R.drawable.logo)
                            .setNeutralButton("CLIENT & MTS",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                String customer_email = emailEditText.getText().toString().replaceAll("\\s+","");

                                                if (isValidEmailAddress(customer_email)) {
                                                    email(getApplicationContext(), new String[]{"mtsbureau@gmail.com",customer_email}, key, "Informations sur " + displaykey + " en pièce jointe", getAttachments(key), getHtml(key));
                                                    db.deleteBon(key);
                                                    listItems.remove(displaykey);
                                                    adapter.notifyDataSetChanged();
                                                    dialog.cancel();
                                                } else {
                                                    email(getApplicationContext(), new String[]{"mtsbureau@gmail.com"}, key, "Informations sur " + displaykey + " en pièce jointe", getAttachments(key), getHtml(key));
                                                    db.deleteBon(key);
                                                    listItems.remove(displaykey);
                                                    adapter.notifyDataSetChanged();
                                                    dialog.cancel();
                                                }
                                                
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                            .setNegativeButton("MTS",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            email(getApplicationContext(), new String[]{"mtsbureau@gmail.com"}, key, "Informations sur " + displaykey + " en pièce jointe", getAttachments(key), getHtml(key));
                                            db.deleteBon(key);
                                            listItems.remove(displaykey);
                                            adapter.notifyDataSetChanged();
                                            dialog.cancel();
                                        }
                                    })
                            .setView(emailEditText)
                            .show();
    }

    public static boolean isValidEmailAddress(String email) {
        String regExpn =
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                        +"((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        +"([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        +"[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                        +"([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(regExpn,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if(matcher.matches())
            return true;
        else
            return false;
    }

    public void email(Context context, String[] emailTo,
                      String subject, String emailText, List<File> filePaths, StringBuilder html)
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
                emailTo);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(
            Intent.EXTRA_TEXT,
            emailText
        );


        //has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        //Compress comp = new Compress(filePaths.toArray(new File[filePaths.size()]),subject);
        // File zip = comp.zip();
        Uri u;

        if (filePaths != null) {
            //convert from paths to Android friendly Parcelable Uri's
            for (File file : filePaths)
            {
                u = Uri.fromFile(file);
                uris.add(u);
            }
        }

        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Aperçus Html");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, subject+"résumé.html");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(html.toString());
            writer.flush();
            writer.close();
            u = Uri.fromFile(gpxfile);
            uris.add(u);
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
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
        Cursor old_pices = null;
        for (int i = 0; i <= getSessionNumber(key);i++) {
            work = db.getWork(key, i);
            wkers = db.getAllWorkers(key, i);
            pices = db.getAllPieces(key, i);
            sess = db.getSession(key, i);
            info += infoText(client,work,sess,wkers,pices,old_pices);
            if (pices.moveToNext() && getSessionNumber(key) == 0) {
                info += addRow("Travail exécuté","");
                info += addRow("Observations","");
                info += addRow("Type de pièce et description", pices.getString(1) + ":" + pices.getString(2));
                info += addRowInt("Quantité", pices.getString(3));

                while(pices.moveToNext()) {
                    info += addRow("Type de pièce et description", pices.getString(1) + ":" + pices.getString(2));
                    info += addRowInt("Quantité", pices.getString(3));
                }
            }

            if(getSessionNumber(key) >= 1) {
                old_pices = pices;
            }
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

    public String infoText(ContentValues cl,ContentValues w, Cursor sess, Cursor ws, Cursor pcs, Cursor old_pcs) {
        if (old_pcs == null) {
            return infoText(cl,w,sess,ws,pcs);
        } else {
            sess.moveToNext();
            String res = "";
            res += addRow("Travail exécuté",w.get("execute").toString());
            res += addRow("Observations",w.get("observations").toString());
            while (old_pcs.moveToNext()) {
                res += addRow("Type de pièce et description", old_pcs.getString(1) + ":" + old_pcs.getString(2));
                res += addRowInt("Quantité", old_pcs.getString(3));
            }
            while (pcs.moveToNext()) {
                res += addRow("Type de pièce et description", pcs.getString(1) + ":" + pcs.getString(2));
                res += addRowInt("Quantité", pcs.getString(3));
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

        for (int i=0; i < 9;i++) {
            if (pcs.moveToNext()) {
                res += addRow("Type de pièce et description", pcs.getString(1) + ":" + pcs.getString(2));
                res += addRowInt("Quantité", pcs.getString(3));
            } else {
                res += addRow("Type de pièce et description","");
                res += addRow("Quantité","");
            }
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
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

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
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"infousr.txt");
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

    public StringBuilder getHtml(String key) {
        int i = 0;
        ContentValues client = db.getClient(key);
        ContentValues work = db.getWork(key, i);
        Cursor wkers = db.getAllWorkers(key, i);
        Cursor pices = db.getAllPieces(key, i);
        Cursor sess = db.getSession(key, i);

        String result = "<html>" + "\n" +
        "<body>" + "\n" +
        "<table cellspacing=\"0\" border=\"0\">" + "\n" +
            "<colgroup width=\"87\"></colgroup>" + "\n" +
            "<colgroup span=\"2\" width=\"72\"></colgroup>" + "\n" +
            "<colgroup span=\"3\" width=\"62\"></colgroup>" + "\n" +
            "<colgroup width=\"70\"></colgroup>" + "\n" +
            "<colgroup width=\"64\"></colgroup>" + "\n" +
            "<colgroup width=\"107\"></colgroup>" + "\n" +
            "<colgroup width=\"88\"></colgroup>" + "\n" +
            "<colgroup span=\"2\" width=\"107\"></colgroup>" + "\n" +
            "<colgroup width=\"112\"></colgroup>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"22\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> REPARATIONS - ENTRETIENS - REMPLACEMENT  </font></td>" + "\n" +
                "<td colspan=3 rowspan=7 align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br><img src=\""+getLogoDataUrl()+"\" width=222 height=124 hspace=19 vspace=15>" + "\n" +
                "</font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 align=\"center\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> BON DE TRAVAIL N° : WERKBON </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"22\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> PORTES ET CHASSIS ALU- ACIER - GLACE TREMPEE  </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 rowspan=2 align=\"center\" sdval=\"0\" sdnum=\"1033;\"><font face=\"Calibri\" size=5 color=\"#FF0000\">"+key+"</font></td>" + "\n" +
            "</tr>" + "\n" +
                "<tr>" + "\n" +
                "<td colspan=7 height=\"22\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> SERRURERIE - FERRONNERIE - PORTES AUTOMATIQUES </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"20\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td colspan=3 rowspan=5 align=\"center\" valign=middle sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" size=4 color=\"#000000\"> MODERN TEAM SERVICES <br>PLACE J. GOFFIN 31 1480 CLABECQ <br>T.V.A. : BE 0440.147.297  </font></b></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"21\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> HERSTELLINGEN - ONDERHOUD - VERVANGINGEN </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"21\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> DEUREN EN OMLIJSTINGEN ALU- STAAL - HARD GLAS  </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"21\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> SLOTEMAKERSWERK - IJZERWERK </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td colspan=7 height=\"21\" align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> AUTOMATISCHE DEUREN </font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=7 height=\"21\" align=\"center\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> LIEU DES PRESTATIONS - PRESTATIE PLAATS </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 align=\"center\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> NOM CLIENT - NAAM </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 align=\"center\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> CODE CLIENT </font></b></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=7 rowspan=2 height=\"64\" align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" size=4 color=\"#0070C0\">"+client.get("lieu").toString()+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 rowspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#0070C0\">"+client.get("name_client").toString()+"</font></b></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=3 rowspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;0\"><b><font face=\"Calibri\" size=4 color=\"#0070C0\">"+client.get("code_client").toString()+"</font></b></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000\" height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> TRAVAIL DEMANDE - GEVRAAD WERK :  </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-right: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-right: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000\" colspan=2 align=\"center\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> Nom Contact :  </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-right: 2px solid #000000\" colspan=4 align=\"center\" bgcolor=\"#FFFFFF\" sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\">"+client.get("contact").toString()+"</font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=13 rowspan=3 height=\"59\" align=\"left\" valign=top sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#0070C0\">"+work.get("demand").toString()+"</font></b></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr></tr><tr></tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-left: 2px solid #000000\" height=\"23\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" size=3 color=\"#000000\"> PREMIER PASSAGE : </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\">N° COMMANDE:</font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000\" colspan=2 align=\"center\" bgcolor=\"#FFFFFF\" sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#0070C0\">"+work.get("facture").toString()+"</font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\">N° DEVIS:</font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 2px solid #000000; border-right: 2px solid #000000\" colspan=2 align=\"center\" bgcolor=\"#FFFFFF\" sdval=\"0\" sdnum=\"1033;\"><font face=\"Calibri\" color=\"#0070C0\">"+work.get("devis").toString()+"</font></td>" + "\n" +
            "</tr>" + "\n";

            result += html_passage(client,sess,work,wkers,pices);
            

        i = getSessionNumber(key);
        if (i > 0) {
            work = db.getWork(key, i);
            wkers = db.getAllWorkers(key, i);
            pices = db.getAllPieces(key, i);
            sess = db.getSession(key, i);

            result += "<tr>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000; border-left: 2px solid #000000\" height=\"1\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-bottom: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-left: 2px solid #000000\" height=\"23\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" size=3 color=\"#000000\">  SECOND PASSAGE : </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\"><br></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-right: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\"><br></td>" + "\n" +
            "</tr>" + "\n";

            result += html_passage(client,sess,work,wkers,pices);
        }
        
        result += "</table>" + "\n" +
            "</body>" + "\n" +
            "</html>";

        return new StringBuilder().append(result);
    }

    public String html_passage(ContentValues client, Cursor sess, ContentValues w, Cursor wkers, Cursor pices) {
        String result = "<tr>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000; border-left: 2px solid #000000\" height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> TRAVAIL EXECUTE - UITGEVOERD WERK </font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\"><br></td>"+ "\n" +
                "<td style=\"border-top: 2px solid #000000; border-right: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\"><br></td>"+ "\n" +
            "</tr>"+ "\n" +
            "<tr>"+ "\n" +
                "<td style=\"border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 2px solid #000000\" colspan=13 rowspan=3 height=\"59\" align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#0070C0\">"+w.get("execute").toString()+"</font></b></td>"+ "\n" +
            "</tr>"+ "\n" +
            "<tr></tr><tr></tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-left: 2px solid #000000\" height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> OBSERVATIONS- OPMERK </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-right: 2px solid #000000\" colspan=8 rowspan=2 align=\"center\" sdval=\"0\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\">"+w.get("observations").toString()+"</font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-left: 2px solid #000000\" height=\"21\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
            "</tr>" + "\n" +
            "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> PIECES REMPLACEES- VERVANGEN STUKKEN </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> code art. </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"> Quantité </font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\"></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" align=\"left\"></td>" + "\n" +
            "</tr>" + "\n";

        int i = 0;
        while(pices.moveToNext()) {
            result += "<tr>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" colspan=9 height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+pices.getString(1)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+pices.getString(2)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"center\" sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\">"+pices.getString(3)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"right\" bgcolor=\"#FFF2CC\" sdval=\"0\" sdnum=\"1033;0;#,##0.00&quot; €&quot;\"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;#,##0.00&quot; €&quot;\"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
            "</tr>" + "\n";
            
            i++;
        }

        result += "<tr>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" colspan=9 height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#00B0F0\"><br></font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"center\" sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"right\" bgcolor=\"#FFF2CC\" sdval=\"0\" sdnum=\"1033;0;#,##0.00&quot; €&quot;\"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;#,##0.00&quot; €&quot;\"><b><font face=\"Calibri\" color=\"#00B0F0\"></font></b></td>" + "\n" +
            "</tr>" + "\n";

        result +=   "<tr>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" height=\"21\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> DATE  </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> Nb Tech  </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" size=1 color=\"#000000\"> INITIALES </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" colspan=2 align=\"center\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> Heure Arrivée </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"> Heure départ </font></b></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-bottom: 1px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" align=\"left\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><b><font face=\"Calibri\" color=\"#000000\"><br></font></b></td>" + "\n" +
                "<td></td>" + "\n" +
                "<td><b><br></font></b></td>" + "\n" +
                "<td><b></b></td>" + "\n" +
                "<td></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
                "<td style=\"border-top: 2px solid #000000; border-right: 2px solid #000000\" align=\"left\" bgcolor=\"#FFFFFF\" sdnum=\"1033;0;_-* #,##0.00&quot; €&quot;_-;-* #,##0.00&quot; €&quot;_-;_-* -??&quot; €&quot;_-;_-@_-\"><font face=\"Calibri\" color=\"#000000\"><br></font></td>" + "\n" +
            "</tr>" + "\n";

        sess.moveToNext();
        String tmp = sess.getString(2);
        result += "<tr>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" height=\"39\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;M/D/YYYY\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+tmp.substring(0, 10)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\">"+w.get("number").toString()+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\">"+client.get("id").toString().substring(0, 2)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" colspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;H:MM;@\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+tmp.substring(11, 16)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" colspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;H:MM;@\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+sess.getString(3).substring(11, 16)+"</font></b></td>" + "\n" +
                "<td></td>" + "\n" +
                "<td></td>" + "\n" +
            "</tr>" + "\n";
        if(wkers.moveToNext()) {
            result += "<tr>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 2px solid #000000; border-right: 1px solid #000000\" height=\"39\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;M/D/YYYY\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+tmp.substring(0, 10)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\">"+wkers.getString(5)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000\" align=\"right\" valign=middle sdval=\"0\" sdnum=\"1033;0;#,##0_ ;-#,##0 \"><b><font face=\"Calibri\" color=\"#00B0F0\">"+wkers.getString(1)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 1px solid #000000\" colspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;H:MM;@\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+wkers.getString(2)+"</font></b></td>" + "\n" +
                "<td style=\"border-top: 1px solid #000000; border-bottom: 2px solid #000000; border-left: 1px solid #000000; border-right: 2px solid #000000\" colspan=2 align=\"center\" valign=middle sdval=\"0\" sdnum=\"1033;0;H:MM;@\"><b><font face=\"Calibri\" color=\"#00B0F0\">"+wkers.getString(3)+"</font></b></td>" + "\n" +
                "<td></td>" + "\n" +
                "<td></td>" + "\n" +
            "</tr>" + "\n";
        }

        return result;
    }

    public String getLogoDataUrl() {
        return "data:image/jpeg;base64,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bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6MThBQzE5RTFFODVGRTIxMTgxOTBDMzUxRTRBMUQ5QUUiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6MmNiNzhhNjQtODUzZC00NWE0LWJkYWQtM2YyOWY5NGYxN2E1IiB4bXBNTTpPcmlnaW5hbERvY3VtZW50SUQ9InhtcC5kaWQ6MThBQzE5RTFFODVGRTIxMTgxOTBDMzUxRTRBMUQ5QUUiIEdJTVA6QVBJPSIyLjAiIEdJTVA6UGxhdGZvcm09IldpbmRvd3MiIEdJTVA6VGltZVN0YW1wPSIxNTMxMTQyMzg4NTkxNjk5IiBHSU1QOlZlcnNpb249IjIuMTAuMiIgZGM6Rm9ybWF0PSJpbWFnZS9qcGVnIiBwaG90b3Nob3A6Q29sb3JNb2RlPSIzIiBwaG90b3Nob3A6SUNDUHJvZmlsZT0ic1JHQiBJRUM2MTk2Ni0yLjEiIHhtcDpDcmVhdGVEYXRlPSIyMDEzLTAxLTE2VDE1OjMxOjQ0KzAxOjAwIiB4bXA6Q3JlYXRvclRvb2w9IkdJTVAgMi4xMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAxMy0wMS0xNlQxNTozMTo0NCswMTowMCIgeG1wOk1vZGlmeURhdGU9IjIwMTMtMDEtMTZUMTU6MzE6NDQrMDE6MDAiPiA8aXB0Y0V4dDpMb2NhdGlvbkNyZWF0ZWQ+IDxyZGY6QmFnLz4gPC9pcHRjRXh0OkxvY2F0aW9uQ3JlYXRlZD4gPGlwdGNFeHQ6TG9jYXRpb25TaG93bj4gPHJkZjpCYWcvPiA8L2lwdGNFeHQ6TG9jYXRpb25TaG93bj4gPGlwdGNFeHQ6QXJ0d29ya09yT2JqZWN0PiA8cmRmOkJhZy8+IDwvaXB0Y0V4dDpBcnR3b3JrT3JPYmplY3Q+IDxpcHRjRXh0OlJlZ2lzdHJ5SWQ+IDxyZGY6QmFnLz4gPC9pcHRjRXh0OlJlZ2lzdHJ5SWQ+IDx4bXBNTTpIaXN0b3J5PiA8cmRmOlNlcT4gPHJkZjpsaSBzdEV2dDphY3Rpb249ImNyZWF0ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6MThBQzE5RTFFODVGRTIxMTgxOTBDMzUxRTRBMUQ5QUUiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCBDUzYgKFdpbmRvd3MpIiBzdEV2dDp3aGVuPSIyMDEzLTAxLTE2VDE1OjMxOjQ0KzAxOjAwIi8+IDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJzYXZlZCIgc3RFdnQ6Y2hhbmdlZD0iLyIgc3RFdnQ6aW5zdGFuY2VJRD0ieG1wLmlpZDoxOUFDMTlFMUU4NUZFMjExODE5MEMzNTFFNEExRDlBRSIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENTNiAoV2luZG93cykiIHN0RXZ0OndoZW49IjIwMTMtMDEtMTZUMTU6MzE6NDQrMDE6MDAiLz4gPHJkZjpsaSBzdEV2dDphY3Rpb249InNhdmVkIiBzdEV2dDpjaGFuZ2VkPSIvIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOmFmNmU5NDMxLWNjZDEtNGFlMS04NWMxLTIxYzliOTk5ZWRhZiIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iR2ltcCAyLjEwIChXaW5kb3dzKSIgc3RFdnQ6d2hlbj0iMjAxOC0wNy0wOVQyMToxOTo0OCIvPiA8L3JkZjpTZXE+IDwveG1wTU06SGlzdG9yeT4gPHBsdXM6SW1hZ2VTdXBwbGllcj4gPHJkZjpTZXEvPiA8L3BsdXM6SW1hZ2VTdXBwbGllcj4gPHBsdXM6SW1hZ2VDcmVhdG9yPiA8cmRmOlNlcS8+IDwvcGx1czpJbWFnZUNyZWF0b3I+IDxwbHVzOkNvcHlyaWdodE93bmVyPiA8cmRmOlNlcS8+IDwvcGx1czpDb3B5cmlnaHRPd25lcj4gPHBsdXM6TGljZW5zb3I+IDxyZGY6U2VxLz4gPC9wbHVzOkxpY2Vuc29yPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAg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";
    }

}
