package com.thomas_dieuzeide.myapplication;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by Thomas_Dieuzeide on 12/27/2015.
 */
public class FragmentPage extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";

    private int mPage;
    boolean existsClient, existsWork = false;
    private DatabaseHelper db;
    private String value;
    private int session, vision;
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    private ArrayList<String> listItems=new ArrayList<String>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    private ArrayAdapter<String> adapter;
    private Context context;

    // CLIENT LAYOUT ELEMENTS
    AutoCompleteTextView actv,actv2;
    CheckBox cb;
    EditText lieu,contact;
    boolean nu = false;
    boolean blip = false;

    // WORK LAYOUT ELEMENTS
    EditText demand,execute,observations,devis,facture;
    Button b1;// additional workers button
    ListView lv;
    String workername;
    boolean repeat;
    boolean add;
    int posWorker;
    int numWorker = 1;
    int quantworker;
    ArrayAdapter<String> ad;
    private Integer[] hours = new Integer[2];
    private Integer[] minutes = new Integer[2];
    private ArrayList<String> workers =new ArrayList<String>();

    //PIECES LAYOUT ELEMENTS
    ArrayAdapter<String> ap;
    private ArrayList<String> pieces =new ArrayList<String>();
    private ArrayList<String> listPieces =new ArrayList<String>();
    String piecetype,piecedesc;
    int posPieces;

    // RESUME ELEMENTS
    boolean correct = true;
    Button bs; // signing button: not visible if form not complete

    // STRINGS FOR DB
    String sclient = "";
    String sclientname = "";
    String scontact = "";
    String slieu = "";
    String sdemand = "";
    String sexecute = "";
    String sobservations = "";
    String sdevis = "";
    String sfacture = "";

    public static Fragment newInstance(int page,String value,int session,int vision) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        args.putString("key", value);
        args.putInt("session", session);
        args.putInt("vision", vision);
        FragmentPage fragment = new FragmentPage();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mPage = getArguments().getInt(ARG_PAGE);
        value = getArguments().getString("key");
        session = getArguments().getInt("session");
        vision = getArguments().getInt("vision");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        if(mPage == 1) {
            view = inflater.inflate(R.layout.activity_client, container, false);
            context = MyApplication.getAppContext();
            db = new DatabaseHelper(context);
            actv = (AutoCompleteTextView) view.findViewById(R.id.editTextClient);
            actv.setThreshold(4);
            actv2 = (AutoCompleteTextView) view.findViewById(R.id.editTextClientName);
            actv2.setThreshold(3);

            contact = (EditText) view.findViewById(R.id.editTextContact);
            contact.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    scontact = contact.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            lieu = (EditText) view.findViewById(R.id.editTextLieu);
            lieu.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    slieu = lieu.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            final String[] codes = getStringFromFile("codeclient.txt");
            final String[] names = getStringFromFile("nomclient.txt");

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,R.layout.my_list_item,codes);
            actv.setAdapter(adapter);
            actv.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sclient = actv.getText().toString();
                    if(Arrays.asList(codes).contains(sclient) && !nu) {
                        String c = names[Arrays.asList(codes).indexOf(sclient)];
                        blip = !blip;
                        if(blip){
                            actv2.setText(c);
                        }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            final ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(context,R.layout.my_list_item,names);
            actv2.setAdapter(adapter2);
            actv2.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sclientname = actv2.getText().toString();
                    if(Arrays.asList(names).contains(sclientname) && !nu) {
                        String c = codes[Arrays.asList(names).indexOf(sclientname)];
                        blip = !blip;
                        if(blip) {
                            actv.setText(c);
                        }
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            Button button= (Button) view.findViewById(R.id.buttonclient);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store();
                }
            });

            cb = (CheckBox) view.findViewById(R.id.checkbox);

            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nu = !nu;
                    if(nu) {
                        actv.setText("999999");
                        actv.setEnabled(false);
                        actv2.setAdapter(null);
                    } else {
                        actv.setEnabled(true);
                        actv.setText("");
                        actv2.setAdapter(adapter2);
                    }
                }
            });

            ContentValues values = db.getClient(value);

            if (values.size() > 0) {
                existsClient = true;
                actv.setText(values.get("code_client").toString());
                sclient = values.get("code_client").toString();
                lieu.setText(values.get("lieu").toString());
                slieu = values.get("lieu").toString();
                actv2.setText(values.get("name_client").toString());
                sclientname = values.get("name_client").toString();
                contact.setText(values.get("contact").toString());
                scontact = values.get("contact").toString();
                String n = values.get("new").toString();
                if(n == "1") {
                    cb.setChecked(true);
                    actv.setEnabled(false);
                    actv2.setAdapter(null);
                    nu = true;
                }
            }

            if(session > 0 || vision > 0) {
                //We block certain fields as this is not the first session
                actv.setEnabled(false);
                actv2.setEnabled(false);
                cb.setEnabled(false);
                lieu.setEnabled(false);
            }
            if(vision > 0) {
                //We block all the fields as this is vision mode
                contact.setEnabled(false);
            }
        } else if(mPage == 2) {
            view = inflater.inflate(R.layout.activity_work, container, false);
            context = MyApplication.getAppContext();
            db = new DatabaseHelper(context);
            lv =(ListView) view.findViewById(R.id.listViewWorker);
            ad = new ArrayAdapter<String>(context, R.layout.my_list_item, listItems);
            lv.setAdapter(ad);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    posWorker = position;
                    addWorker(workers.get(position));
                }
            });
            demand = (EditText) view.findViewById(R.id.editTextDemand);
            demand.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sdemand = demand.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            execute = (EditText) view.findViewById(R.id.editTextExecute);
            execute.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sexecute = execute.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            observations = (EditText) view.findViewById(R.id.editTextObservations);
            observations.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sobservations = observations.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            devis = (EditText) view.findViewById(R.id.editTextDevis);
            devis.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sdevis = devis.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            facture = (EditText) view.findViewById(R.id.editTextFacture);
            facture.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    sfacture = facture.getText().toString();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            Button button= (Button) view.findViewById(R.id.buttonNext);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store();
                }
            });

            Spinner dropdown = (Spinner) view.findViewById(R.id.spinnerwork);
            String[] items = new String[]{"1", "2", "3"};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.my_list_item, items);
            dropdown.setAdapter(adapter);

            dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    numWorker = position + 1;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }

            });
            b1 = (Button) view.findViewById(R.id.buttonworker);
            b1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(workers.size() < 2) {
                        addNewWorker();
                    } else {
                        Toast.makeText(getActivity(),"TRAVAILLEUR(S) ADDITIONELS EXISTANT", Toast.LENGTH_LONG).show();
                    }
                }
            });

            Cursor c = db.getAllWorkers(value,session);
            while (c.moveToNext()) {
                if (!workers.contains(c.getString(1))){
                    workers.add(c.getString(1));
                    listItems.add("INITIALES: " + c.getString(1) + ", QUANTITÉ: " + c.getString(4) + ", ARRIVÉE: " + c.getString(2) + ", DÉPART: " + c.getString(3));
                }
            }

            ad.notifyDataSetChanged();

            ContentValues values = db.getWork(value,session);
            if (values.size() > 0) {
                existsWork = true;
                demand.setText(values.get("demand").toString());
                sdemand = values.get("demand").toString();
                execute.setText(values.get("execute").toString());
                sexecute = values.get("execute").toString();
                observations.setText(values.get("observations").toString());
                sobservations = values.get("observations").toString();
                numWorker = Integer.valueOf(values.get("number").toString());
                dropdown.setSelection(numWorker - 1);
                devis.setText(values.get("devis").toString());
                sdevis = values.get("devis").toString();
                facture.setText(values.get("facture").toString());
                sfacture=values.get("facture").toString();
            }

            if(session > 0) {
                //We block certain fields as this is not the first session
                demand.setEnabled(false);
            }
            if(vision > 0) {
                //We block all the fields as this is vision mode
                demand.setEnabled(false);
                execute.setEnabled(false);
                observations.setEnabled(false);
                dropdown.setEnabled(false);
                devis.setEnabled(false);
                facture.setEnabled(false);
                b1.setVisibility(View.GONE);
            }
        } else if(mPage == 3) {
            view = inflater.inflate(R.layout.activity_pieces, container, false);
            context = MyApplication.getAppContext();
            db = new DatabaseHelper(context);

            Button b = (Button) view.findViewById(R.id.buttonreturn);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store();
                }
            });

            Button b2 = (Button) view.findViewById(R.id.buttonpiece);
            b2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addPiece(null);
                }
            });
            ListView lp = (ListView) view.findViewById(R.id.listViewpieces);
            ap = new ArrayAdapter<String>(context, R.layout.my_list_item, listPieces);
            lp.setAdapter(ap);
            lp.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    String[] parts = pieces.get(position).split("§");
                    posPieces = position;
                    addPiece(parts);
                }
            });
            Cursor c = db.getAllPieces(value,session);
            while (c.moveToNext()) {
                if(!pieces.contains(c.getString(1)+"§"+c.getString(2)+"§"+c.getInt(3))) {
                    pieces.add(c.getString(1)+"§"+c.getString(2)+"§"+c.getInt(3));
                    listPieces.add("TYPE: " + c.getString(1) + "\n DESCRIPTION: " + c.getString(2)+"\n QUANTITE: " + c.getInt(3));
                }
            }
            if (vision > 0) {
                b2.setVisibility(View.GONE);
                lp.setOnItemClickListener(null);
            }
            ap.notifyDataSetChanged();
        } else if(mPage == 4) {
            view = inflater.inflate(R.layout.activity_resume, container, false);
            context = MyApplication.getAppContext();
            db = new DatabaseHelper(context);

            correct = true;

            bs = (Button) view.findViewById(R.id.buttonSign);
            bs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), CaptureSignature.class);
                    intent.putExtra("key",value);
                    intent.putExtra("session",session);
                    startActivity(intent);
                    getActivity().finish();
                }
            });


            ContentValues client = db.getClient(value);
            ContentValues work = db.getWork(value,session);
            Cursor wkers = db.getAllWorkers(value, session);
            Cursor pices = db.getAllPieces(value, session);
            Cursor sess = db.getSession(value,session);

            TextView tvr = (TextView) view.findViewById(R.id.textViewResume);
            tvr.setGravity(Gravity.CENTER);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            infoText(client,work,sess,wkers,pices,builder);
            if (vision > 0) {
                bs.setVisibility(View.GONE);
                String s = "ON NE PEUT PAS SIGNER EN MODE VISIONNAGE! \n";
                SpannableString st = new SpannableString(s);
                st.setSpan(new ForegroundColorSpan(Color.BLUE), 0, s.length(), 0);
                builder.insert(0, st);
            } else {
                if(correct) {
                    bs.setVisibility(View.VISIBLE);
                    String s = "BON COMPLET! PRÊT À SIGNER! \n";
                    SpannableString st = new SpannableString(s);
                    st.setSpan(new ForegroundColorSpan(Color.GREEN), 0, s.length(), 0);
                    builder.insert(0,st);

                } else {
                    bs.setVisibility(View.GONE);
                    String s = "BON INCOMPLET! \n";
                    SpannableString st = new SpannableString(s);
                    st.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
                    builder.insert(0,st);
                }
            }

            tvr.setText(builder, TextView.BufferType.SPANNABLE);
        }
        return view;
    }

    public void store() {
        if(mPage == 1) {
            if (existsClient) {
                db.updateClient(sclient, sclientname, value, slieu,nu,scontact);
            }
            else {
                db.addClient(sclient, sclientname, value, slieu,nu,scontact);
            }
        } else if (mPage == 2) {
            if (existsWork) {
                db.updateWork(value, sdemand, sexecute, sobservations,numWorker,sdevis,sfacture,session);
            }
            else {
                db.addWork(value, sdemand, sexecute, sobservations,numWorker,sdevis,sfacture,session);
            }
        }
        getActivity().finish();
    }

    public int getMPage() {
        return mPage;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if(mPage == 1) {
                    if (existsClient && (sclient!="" || sclientname!="")) {
                        db.updateClient(sclient, sclientname, value, slieu,nu,scontact);
                    }
                    else {
                        db.addClient(sclient, sclientname, value, slieu,nu,scontact);
                    }
                } else if (mPage == 2) {
                    if (existsWork && (sdemand!="" || sclientname!="")) {
                        db.updateWork(value, sdemand, sexecute, sobservations,numWorker,sdevis,sfacture,session);
                    }
                    else {
                        db.addWork(value, sdemand, sexecute, sobservations,numWorker,sdevis,sfacture,session);
                    }
                }
            }
        }
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
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        return text.toArray(new String[text.size()]);
    }

    public void addNewWorker() {
        addWorker(null);
    }

    public void addWorker(String name) {
        final String nom = name;
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("NOUVEAU(X) TRAVAILLEUR(S)"); //Set Alert dialog title here
        alert.setMessage("INITIALES:"); //Message here
        alert.setIcon(R.drawable.logo);

        // Set an EditText view to get user input
        final NumberPicker input = new NumberPicker(getActivity());
        input.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        input.setMinValue(0);
        input.setMaxValue(5);
        final String[] names = getStringFromFile("workers.txt");
        input.setDisplayedValues(names);
        if(name != null) {
            input.setValue(Arrays.asList(names).indexOf(name));
        }
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                workername = input.getDisplayedValues()[input.getValue()];
                if (workername.length() > 3) {
                    Toast.makeText(getActivity(), "Entrez les initiales", Toast.LENGTH_SHORT).show();
                    addWorker(nom);
                } else {
                    addQuantity(nom);
                }
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("RETOUR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void addQuantity(String name) {
        final String nom = name;
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("NOUVEAU(X) TRAVAILLEUR(S)"); //Set Alert dialog title here
        alert.setMessage("QUANTITE:"); //Message here
        alert.setIcon(R.drawable.logo);

        final NumberPicker input = new NumberPicker(getActivity());
        input.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        input.setMinValue(1);
        input.setMaxValue(3);

        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                addEndTime(nom);
                quantworker = input.getValue();
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("RETOUR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void addStartTime() {
        TimePickerDialog.OnTimeSetListener timePickerListener =  new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute){
                hours[0] = selectedHour;
                minutes[0] = selectedMinute;
            }
        };
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog tpd = new TimePickerDialog(getActivity(), timePickerListener, hour, minute, true);
        tpd.setCanceledOnTouchOutside(false);
        tpd.setIcon(R.drawable.logo);
        tpd.setTitle("DÉFINIR L'HEURE");
        tpd.setMessage("HEURE DE DÉBUT");
        tpd.show();
    }

    public void addEndTime(String name) {
        final String nom = name;
        repeat = false;
        add = false;
        TimePickerDialog.OnTimeSetListener timePickerListener =  new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute){
                if(!repeat || add) {
                    hours[1] = selectedHour;
                    minutes[1] = selectedMinute;
                    repeat = true;
                    if(hoursOK()) {

                        String twodigit0, twodigit1;
                        if(minutes[0].toString().length() == 1) {
                            twodigit0 = "0"+minutes[0].toString();
                        } else {
                            twodigit0 = minutes[0].toString();
                        }

                        if(minutes[1].toString().length() == 1) {
                            twodigit1 = "0"+minutes[1].toString();
                        } else {
                            twodigit1 = minutes[1].toString();
                        }

                        if(nom == null) {
                            db.addWorker(value, workername, hours[0] + ":" + twodigit0, hours[1] + ":" + twodigit1, session, quantworker);


                            listItems.add("INITIALES: " + workername + ", QUANTITE: " + quantworker+ ", ARRIVÉE: " + hours[0] + ":" + twodigit0 + ", DÉPART: " + hours[1] + ":" + twodigit1);
                            workers.add(workername);
                        } else {
                            listItems.set(posWorker,"INITIALES: " + workername + ", QUANTITE: " + quantworker + ", ARRIVÉE: " + hours[0] + ":" + twodigit0 + ", DÉPART: " + hours[1] + ":" +twodigit1);
                            workers.set(posWorker,workername);
                            db.updateWorker(value,nom,workername,hours[0] + ":" +twodigit0, hours[1] + ":" +twodigit1,session,quantworker);
                        }
                        ad.notifyDataSetChanged();
                        add = false;
                    } else if(!add){
                        Toast.makeText(getActivity(), "Heures incorrectes", Toast.LENGTH_SHORT).show();
                        addEndTime(nom);
                        add = true;
                    }
                }
            }
        };
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog tpd = new TimePickerDialog(getActivity(), timePickerListener, hour, minute,true);
        tpd.setCanceledOnTouchOutside(false);
        tpd.setIcon(R.drawable.logo);
        tpd.setTitle("DÉFINIR L'HEURE");
        tpd.setMessage("HEURE DE FIN");
        tpd.show();
        addStartTime();
    }

    public boolean hoursOK() {
        if(hours[1] < hours[0]) {
            return false;
        } else if (hours[1] == hours[0] && minutes[1] <= minutes[0]) {
            return false;
        }
        return true;
    }

    public void addPiece(String[] type) {
        final String[] nom = type;
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("NOUVELLE PIECE"); //Set Alert dialog title here
        alert.setMessage("TYPE DE LA PIECE:"); //Message here
        alert.setIcon(R.drawable.logo);

        // Set an EditText view to get user input
        final AutoCompleteTextView input = new AutoCompleteTextView(getActivity());
        final String[] p = getStringFromFile("pieces.txt");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,R.layout.my_list_item,p);
        input.setAdapter(adapter);
        if(type != null) {
            input.setText(type[0]);
        }
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                piecetype = input.getEditableText().toString();
                if (!Arrays.asList(p).contains(piecetype)) {
                    Toast.makeText(getActivity(), "TYPE INEXISTANT", Toast.LENGTH_SHORT).show();
                    addPiece(nom);
                } else {
                    addPieceDescription(nom);
                }
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("RETOUR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void addPieceDescription(String[] type) {
        final String[] nom = type;
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("NOUVELLE PIECE"); //Set Alert dialog title here
        alert.setMessage("DESCRIPTION DE LA PIECE:"); //Message here
        alert.setIcon(R.drawable.logo);

        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());

        if(type != null) {
            input.setText(type[1]);
        }
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                String description = input.getEditableText().toString();
                if (description.length() <= 1) {
                    Toast.makeText(getActivity(), "Description obligatoire!", Toast.LENGTH_SHORT).show();
                    addPieceDescription(nom);
                } else {
                    piecedesc = description;
                    addPieceQuantity(nom);
                }
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("RETOUR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void addPieceQuantity(String[] type) {
        final String[] nom = type;

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("NOUVELLE PIECE"); //Set Alert dialog title here
        alert.setMessage("QUANTITE DE PIECE(S):"); //Message here
        alert.setIcon(R.drawable.logo);

        final NumberPicker input = new NumberPicker(getActivity());
        input.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        input.setMinValue(1);
        input.setMaxValue(30);

        if(type != null) {
            input.setValue(Integer.valueOf(type[2]));
        }
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //You will get as string input data in this variable.
                // here we convert the input to a string and show in a toast.
                int quantity = input.getValue();
                if (nom == null) {
                    db.addPiece(value, piecetype, piecedesc, quantity, session);
                    pieces.add(piecetype + "§" + piecedesc + "§" + quantity);
                    listPieces.add("TYPE: " + piecetype + "\n DESCRIPTION: " + piecedesc + "\n QUANTITE: " + quantity);
                } else {
                    db.updatePiece(value, nom[0], piecetype, nom[1], piecedesc, Integer.valueOf(nom[2]), quantity, session);
                    pieces.set(posPieces, piecetype + "§" + piecedesc + "§" + quantity);
                    listPieces.set(posPieces, "TYPE: " + piecetype + "\n DESCRIPTION: " + piecedesc + "\n QUANTITE: " + quantity);
                }
                ap.notifyDataSetChanged();
            } // End of onClick(DialogInterface dialog, int whichButton)
        }); //End of alert.setPositiveButton
        alert.setNegativeButton("RETOUR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                dialog.cancel();
            }
        }); //End of alert.setNegativeButton
        AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void infoText(ContentValues cl,ContentValues w, Cursor sess, Cursor ws, Cursor pcs, SpannableStringBuilder res) {

        addSpannable( "BON: "+ cl.get("id"),res,Color.BLACK);

        if(cl.get("lieu").toString().length() <= 2) {
            addSpannable("\n\n LIEU DE PRESTATION (INCORRECT): " +cl.get("lieu"),res,Color.RED);
            correct = false;
        } else {
            addSpannable("\n\n LIEU DE PRESTATION: " +cl.get("lieu"),res,Color.BLACK);
        }

        if(cl.get("name_client").toString().length() <= 2) {
            addSpannable("\n NOM CLIENT (INCORRECT): " +cl.get("name_client"),res,Color.RED);
            correct = false;
        } else {
            addSpannable("\n NOM CLIENT: " +cl.get("name_client"),res,Color.BLACK);
        }

        if(cl.get("code_client").toString().length() != 6) {
            addSpannable( "\n CODE CLIENT (INCORRECT): " +cl.get("code_client"),res,Color.RED);
            correct = false;
        } else {
            addSpannable( "\n CODE CLIENT: " +cl.get("code_client"),res,Color.BLACK);
        }

        if(cl.get("contact").toString().length() >= 0) {
            addSpannable( "\n NOM CONTACT: " +cl.get("contact"),res,Color.BLACK);
        }

        if(w.get("demand").toString().length() <= 2) {
            addSpannable("\n\n TRAVAIL DEMANDÉ (INCORRECT): " +w.get("demand"),res,Color.RED);
            correct = false;
        } else {
            addSpannable("\n\n TRAVAIL DEMANDÉ: " +w.get("demand"),res,Color.BLACK);
        }

        if(w.get("execute").toString().length() <= 2) {
            addSpannable("\n TRAVAIL EXÉCUTÉ (INCORRECT): " +w.get("execute"),res,Color.RED);
            correct = false;
        } else {
            addSpannable("\n TRAVAIL EXÉCUTÉ: " +w.get("execute"),res,Color.BLACK);
        }

        if(w.get("observations").toString().length() < 1) {
            addSpannable("\n PAS D'OBSERVATIONS",res,Color.BLACK);
        } else {
            addSpannable("\n OBSERVATIONS: " +w.get("observations"),res,Color.BLACK);
        }

        if(w.get("devis").toString().length() > 1) {
            addSpannable( "\n NUMÉRO DEVIS: " + w.get("devis").toString(),res,Color.BLACK);
        }

        if(w.get("facture").toString().length() > 1) {
            addSpannable( "\n NUMÉRO COMMANDE: " + w.get("facture").toString(),res,Color.BLACK);
        }
        int i = 1;
        if(pcs.moveToNext()) {
            addSpannable("\n\n PIECES: ",res,Color.BLACK);

            addSpannable("\n"+ i+") TYPE"+ pcs.getString(1) + "\nDESCRIPTION: "+ pcs.getString(2)+ "\nQUANTITÉ: "+ pcs.getString(3),res,Color.BLACK);
            i ++;
            while(pcs.moveToNext()){
                addSpannable("\n\n"+ i+") TYPE"+ pcs.getString(1) + "\nDESCRIPTION: "+ pcs.getString(2)+ "\nQUANTITÉ: "+ pcs.getString(3),res,Color.BLACK);
                i ++;
            }
        }

        java.util.Date dt = new java.util.Date();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(dt);
        addSpannable( "\n\n DATE DE LA PRESTATION: " + currentTime.substring(0, 9),res,Color.BLACK);

        addSpannable("\n\n NOMBRE TECHNICIEN(S): " + w.get("number"), res, Color.BLACK);
        sess.moveToNext();
        addSpannable( "\n\n DÉBUT DE LA PRESTATION: " + sess.getString(2).substring(11, 16),res,Color.BLACK);
        System.out.println(sess.getString(1) + " " + sess.getString(2) + " " + sess.getString(3) + " ");
        if (sess.getString(3) != null) {
            addSpannable( "\n FIN DE LA PRESTATION: " + sess.getString(3).substring(11, 16),res,Color.BLACK);
        } else {
            addSpannable( "\n FIN DE LA PRESTATION: " + currentTime.substring(11, 16),res,Color.BLACK);
        }

        if(ws.moveToNext()) {
            addSpannable( "\n\nTRAVAILLEURS ADDITIONELS:\nINITIALES: "+ws.getString(1),res,Color.BLACK);
            addSpannable( "\nQUANTITÉ: "+ws.getString(5),res,Color.BLACK);
            addSpannable( "\nINITIALES: "+ws.getString(1),res,Color.BLACK);
            addSpannable( "\nARRIVÉE: "+ws.getString(2),res,Color.BLACK);
            addSpannable( "\nDÉPART: "+ws.getString(3),res,Color.BLACK);
        }
    }

    public void addSpannable(String s,SpannableStringBuilder b, int c) {
        SpannableString spannable= new SpannableString(s);
        spannable.setSpan(new ForegroundColorSpan(c), 0, s.length(), 0);
        b.append(spannable);
    }

}