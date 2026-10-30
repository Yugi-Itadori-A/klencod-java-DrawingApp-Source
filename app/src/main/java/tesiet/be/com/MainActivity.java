package tesiet.be.com;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG=0xFF0D1117, SURFACE=0xFF161B22, ALT=0xFF21262D,
            PRIMARY=0xFF1F6FEB, TEXT=0xFFE6EDF3, MUTED=0xFF8B949E;
    private DrawingView board;
    private int selectedColor=Color.RED, selectedBg=Color.WHITE;
    private float brush=10f;
    private boolean eraser=false;
    private TextView toolStatus;
    private final int[] palette={Color.BLACK,Color.RED,0xFFFF7B72,0xFF7EE787,0xFF58A6FF,
            0xFFD2A8FF,0xFFFFD33D,0xFFFF9E64,Color.WHITE};
    private final String[] bgNames={"أبيض","أسود","شفاف","وردي فاتح","أزرق فاتح"};
    private final int[] backgrounds={Color.WHITE,Color.BLACK,Color.TRANSPARENT,0xFFFFF0F0,0xFFEAF4FF};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        buildUi();
        board.loadJson(getPreferences(0).getString("autosave",""));
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(TEXT);t.setTextSize(13);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(TEXT);b.setPadding(dp(8),0,dp(8),0);b.setBackground(round(ALT,10));return b;}
    private android.graphics.drawable.GradientDrawable round(int c,int r){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),0xFF30363D);return d;
    }
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(12),dp(8),dp(12),dp(8));header.setBackgroundColor(SURFACE);
        TextView title=label("🎨 لوحة الرسم");title.setTextSize(17);title.setTypeface(null,1);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button undo=button("↶");undo.setOnClickListener(v->board.undo());header.addView(undo);
        Button redo=button("↷");redo.setOnClickListener(v->board.redo());header.addView(redo);
        Button clear=button("مسح");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("مسح اللوحة؟").setMessage("سيتم حذف الرسم الحالي.").setPositiveButton("مسح",(d,w)->board.clear()).setNegativeButton("إلغاء",null).show());header.addView(clear);
        root.addView(header);
        board=new DrawingView();LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,0,1);bp.setMargins(dp(12),dp(10),dp(12),dp(8));root.addView(board,bp);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(12),dp(8),dp(12),dp(10));panel.setBackgroundColor(SURFACE);
        toolStatus=label("الأداة: فرشاة");panel.addView(toolStatus);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        Button brushBtn=button("فرشاة");brushBtn.setOnClickListener(v->{eraser=false;updateTool();});row.addView(brushBtn,new LinearLayout.LayoutParams(0,dp(42),1));
        Button eraserBtn=button("ممحاة");eraserBtn.setOnClickListener(v->{eraser=true;updateTool();});row.addView(eraserBtn,new LinearLayout.LayoutParams(0,dp(42),1));
        Button colorBtn=button("اختيار لون");colorBtn.setOnClickListener(v->colorDialog());row.addView(colorBtn,new LinearLayout.LayoutParams(0,dp(42),1));
        Button bgBtn=button("الخلفية");bgBtn.setOnClickListener(v->backgroundDialog());row.addView(bgBtn,new LinearLayout.LayoutParams(0,dp(42),1));
        panel.addView(row);
        panel.addView(label("حجم الفرشاة"));
        SeekBar size=new SeekBar(this);size.setMax(99);size.setProgress(9);size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){brush=2+p;board.setBrush(brush);}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });panel.addView(size);
        LinearLayout row2=new LinearLayout(this);row2.setGravity(Gravity.CENTER_VERTICAL);
        Button save=button("حفظ المشروع");save.setOnClickListener(v->saveProjectDialog());row2.addView(save,new LinearLayout.LayoutParams(0,dp(44),1));
        Button open=button("مشاريعي");open.setOnClickListener(v->openProjectDialog());row2.addView(open,new LinearLayout.LayoutParams(0,dp(44),1));
        Button export=button("تصدير");export.setOnClickListener(v->exportDialog());row2.addView(export,new LinearLayout.LayoutParams(0,dp(44),1));
        Button share=button("مشاركة");share.setOnClickListener(v->shareImage());row2.addView(share,new LinearLayout.LayoutParams(0,dp(44),1));
        panel.addView(row2);
        TextView zoom=label("التكبير والتصغير: استخدم إصبعين. اسحب بإصبع واحد للرسم.");zoom.setTextSize(11);zoom.setTextColor(MUTED);panel.addView(zoom);
        root.addView(panel);setContentView(root);
    }
    private void updateTool(){toolStatus.setText(eraser?"الأداة: ممحاة":"الأداة: فرشاة");board.setEraser(eraser);}
    private void colorDialog(){
        String[] names={"أسود","أحمر","مرجاني","أخضر","أزرق","بنفسجي","أصفر","برتقالي","أبيض","لون مخصص…"};
        new AlertDialog.Builder(this).setTitle("اختيار اللون").setItems(names,(d,w)->{
            if(w==9){customColorDialog();}else{selectedColor=palette[w];eraser=false;updateTool();board.setColor(selectedColor);}
        }).show();
    }
    private void customColorDialog(){
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(20),dp(8),dp(20),dp(8));box.setOrientation(1);
        EditText hex=new EditText(this);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setText("#"+Integer.toHexString(selectedColor).substring(2));box.addView(hex);
        new AlertDialog.Builder(this).setTitle("لون مخصص").setView(box).setPositiveButton("اختيار",(d,w)->{
            try{selectedColor=Color.parseColor(hex.getText().toString().trim());eraser=false;updateTool();board.setColor(selectedColor);}
            catch(Exception e){toast("صيغة اللون غير صحيحة");}
        }).setNegativeButton("إلغاء",null).show();
    }
    private void backgroundDialog(){
        new AlertDialog.Builder(this).setTitle("خلفية اللوحة").setItems(bgNames,(d,w)->{selectedBg=backgrounds[w];board.setBoardBackground(selectedBg);}).show();
    }
    private void saveProjectDialog(){
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("اسم المشروع");
        new AlertDialog.Builder(this).setTitle("حفظ المشروع").setView(name).setPositiveButton("حفظ",(d,w)->{
            String n=name.getText().toString().trim();if(n.isEmpty())n="رسم "+System.currentTimeMillis();
            try{File dir=new File(getFilesDir(),"projects");dir.mkdirs();File f=new File(dir,safe(n)+".json");
                write(f,board.toJson());toast("تم حفظ المشروع");}
            catch(Exception e){toast("تعذر حفظ المشروع: "+e.getMessage());}
        }).setNegativeButton("إلغاء",null).show();
    }
    private String safe(String s){return s.replaceAll("[^\\p{L}\\p{N}_ -]","_");}
    private void openProjectDialog(){
        File dir=new File(getFilesDir(),"projects");File[] fs=dir.listFiles((f,n)->n.endsWith(".json"));
        if(fs==null||fs.length==0){toast("لا توجد مشاريع محفوظة");return;}
        String[] names=new String[fs.length];for(int i=0;i<fs.length;i++)names[i]=fs[i].getName().replace(".json","");
        new AlertDialog.Builder(this).setTitle("فتح مشروع").setItems(names,(d,w)->{
            try{board.loadJson(read(fs[w]));toast("تم فتح المشروع");}catch(Exception e){toast("تعذر فتح المشروع");}
        }).setNegativeButton("إلغاء",null).show();
    }
    private void exportDialog(){
        new AlertDialog.Builder(this).setTitle("صيغة التصدير").setItems(new String[]{"PNG","JPG"},(d,w)->exportImage(w==0)).show();
    }
    private void exportImage(boolean png){
        try{
            Bitmap bmp=board.exportBitmap(png);String file="Drawing_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+(png?".png":".jpg");
            ContentValues cv=new ContentValues();cv.put(MediaStore.Images.Media.DISPLAY_NAME,file);cv.put(MediaStore.Images.Media.MIME_TYPE,png?"image/png":"image/jpeg");
            if(Build.VERSION.SDK_INT>=29)cv.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/KlencodDrawings");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
            if(uri==null)throw new IOException("تعذر إنشاء الملف");
            OutputStream out=getContentResolver().openOutputStream(uri);if(out==null)throw new IOException("تعذر فتح الملف");
            bmp.compress(png?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,png?100:95,out);out.close();bmp.recycle();toast("تم التصدير إلى الصور");
        }catch(Exception e){toast("فشل التصدير: "+e.getMessage());}
    }
    private void shareImage(){
        try{
            Bitmap bmp=board.exportBitmap(true);File f=new File(getCacheDir(),"shared_drawing.png");FileOutputStream o=new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG,100,o);o.close();bmp.recycle();
            Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".provider",f);
            Intent in=new Intent(Intent.ACTION_SEND);in.setType("image/png");in.putExtra(Intent.EXTRA_STREAM,uri);in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(in,"مشاركة الرسم"));
        }catch(Exception e){toast("تعذرت المشاركة: "+e.getMessage());}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void write(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f);o.write(s.getBytes("UTF-8"));o.close();}
    private String read(File f)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();FileInputStream i=new FileInputStream(f);byte[] a=new byte[4096];int n;while((n=i.read(a))!=-1)b.write(a,0,n);i.close();return b.toString("UTF-8");}
    @Override protected void onPause(){super.onPause();getPreferences(0).edit().putString("autosave",board==null?"":board.toJson()).apply();}

    private class DrawingView extends View {
        private final ArrayList<Stroke> strokes=new ArrayList<>(),redo=new ArrayList<>();
        private Stroke active;private int color=Color.RED,bgColor=Color.WHITE;private float width=10f;
        private float scale=1f,tx=0f,ty=0f,lastX,lastY;private ScaleGestureDetector scaleDetector;private boolean multi=false;
        DrawingView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            scaleDetector=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                public boolean onScale(ScaleGestureDetector d){float old=scale;scale=Math.max(.5f,Math.min(5f,scale*d.getScaleFactor()));float fx=d.getFocusX(),fy=d.getFocusY();tx=fx-(fx-tx)*(scale/old);ty=fy-(fy-ty)*(scale/old);invalidate();return true;}
            });}
        void setColor(int c){color=c;}void setBrush(float w){width=w;}void setEraser(boolean e){}
        void setBoardBackground(int c){bgColor=c;invalidate();}
        void clear(){strokes.clear();redo.clear();active=null;invalidate();}
        void undo(){if(!strokes.isEmpty()){redo.add(strokes.remove(strokes.size()-1));invalidate();}}
        void redo(){if(!redo.isEmpty()){strokes.add(redo.remove(redo.size()-1));invalidate();}}
        @Override protected void onDraw(Canvas c){super.onDraw(c);drawScene(c,false);}
        private void drawScene(Canvas c,boolean export){
            int save=c.save();if(!export){c.translate(tx,ty);c.scale(scale,scale);}
            c.saveLayer(0,0,getWidth(),getHeight(),null);
            if(bgColor==Color.TRANSPARENT){
                if(export) { if(exportJpeg) c.drawColor(Color.WHITE); else c.drawColor(Color.TRANSPARENT); }
                else { Paint p=new Paint(); c.drawColor(Color.WHITE); p.setColor(0xFFE8E8E8);
                    for(int y=0;y<getHeight();y+=dp(18)) for(int x=0;x<getWidth();x+=dp(18))
                        if(((x+y)/dp(18))%2==0)c.drawRect(x,y,x+dp(18),y+dp(18),p); }
            } else c.drawColor(bgColor);
            for(Stroke s:strokes)s.draw(c);if(active!=null)active.draw(c);
            c.restore();c.restoreToCount(save);
        }
        private boolean exportJpeg=false;
        Bitmap exportBitmap(boolean png){
            exportJpeg=!png;
            Bitmap b=Bitmap.createBitmap(Math.max(1,getWidth()),Math.max(1,getHeight()),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
            if(!png && bgColor==Color.TRANSPARENT) c.drawColor(Color.WHITE);
            drawScene(c,true);
            return b;
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            scaleDetector.onTouchEvent(e);
            if(e.getPointerCount()>1||scaleDetector.isInProgress()){multi=true;active=null;return true;}
            float x=(e.getX()-tx)/scale,y=(e.getY()-ty)/scale;
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:multi=false;lastX=x;lastY=y;active=new Stroke(eraser?Color.TRANSPARENT:color,width,eraser);active.add(x,y);invalidate();return true;
                case MotionEvent.ACTION_MOVE:if(active!=null){active.add(x,y);invalidate();}return true;
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:if(active!=null){strokes.add(active);active=null;redo.clear();invalidate();}return true;
            }return true;
        }
        String toJson(){
            try{JSONObject root=new JSONObject();root.put("background",bgColor);JSONArray arr=new JSONArray();
                for(Stroke s:strokes){JSONObject o=new JSONObject();o.put("color",s.color);o.put("width",s.width);o.put("eraser",s.erase);JSONArray pts=new JSONArray();for(float[] p:s.points){JSONArray xy=new JSONArray();xy.put(p[0]);xy.put(p[1]);pts.put(xy);}o.put("points",pts);arr.put(o);}
                root.put("strokes",arr);return root.toString();
            }catch(Exception e){return "{}";}
        }
        void loadJson(String json){
            if(json==null||json.isEmpty())return;try{JSONObject root=new JSONObject(json);bgColor=root.optInt("background",Color.WHITE);strokes.clear();redo.clear();JSONArray a=root.getJSONArray("strokes");
                for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Stroke s=new Stroke(o.optInt("color",Color.BLACK),(float)o.optDouble("width",10),o.optBoolean("eraser",false));JSONArray p=o.getJSONArray("points");for(int j=0;j<p.length();j++){JSONArray xy=p.getJSONArray(j);s.add((float)xy.getDouble(0),(float)xy.getDouble(1));}strokes.add(s);}invalidate();
            }catch(Exception e){toast("تعذر استعادة الحفظ التلقائي");}
        }
        class Stroke{
            int color;float width;boolean erase;ArrayList<float[]> points=new ArrayList<>();
            Stroke(int c,float w,boolean er){color=c;width=w;erase=er;}
            void add(float x,float y){points.add(new float[]{x,y});}
            void draw(Canvas c){Paint p=new Paint(3);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
                if(erase){p.setColor(Color.TRANSPARENT);p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));}else p.setColor(color);
                Path path=new Path();if(points.size()>0){path.moveTo(points.get(0)[0],points.get(0)[1]);for(int i=1;i<points.size();i++)path.lineTo(points.get(i)[0],points.get(i)[1]);}
                c.drawPath(path,p);p.setXfermode(null);
            }
        }
    }
}