package app.hx.viewonce;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Injected at index 0 of X/0lJh->Fs7(LX/00n3;)V, the view-once "media loaded"
 * callback registered through IgProgressImageView.A0C(LX/0Elt;I).
 *
 * Lookups are by type / class-name fragment, never by an obfuscated field name,
 * so a rebuild only needs the two anchors Instagram keeps stable:
 *   - the callback object holds a reference to DirectVisualMessageViewerController
 *   - the controller holds "contentHolder" (LX/0ix0) and "videoPlayer"
 */
public final class Saver {

    private static final String TAG = "HX-VO";
    private static final String SUBDIR = Environment.DIRECTORY_PICTURES + "/InstagramViewOnce";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MIN_SIDE = 200;          // px, below this it is chrome/avatar
    private static long lastHit;
    private static boolean dumpedGraph;

    private static final String CONTROLLER_HINT = "DirectVisualMessageViewerController";

    public static void onMediaLoaded(final Object callback, final Object model) {
        long now = System.currentTimeMillis();
        if (now - lastHit < 1500L) return;
        lastHit = now;
        Log.i(TAG, "hit cb=" + name(callback) + " model=" + name(model));
        MAIN.postDelayed(new Runnable() {
            public void run() {
                try {
                    capture(callback, model);
                } catch (Throwable t) {
                    Log.e(TAG, "capture failed: " + t, t);
                }
            }
        }, 900L);
    }

    private static void capture(Object callback, Object model) {
        Object controller = findController(callback);
        Log.i(TAG, "controller=" + name(controller));
        if (controller == null) {
            dump(callback, "callback");
            return;
        }
        Context ctx = contextOf(controller);
        if (ctx == null) ctx = contextOf(callback);
        if (ctx == null) {
            Log.e(TAG, "no Context reachable");
            return;
        }

        // 1) video: real stream beats a poster frame
        String video = findUrlByFragment(controller, 7, "VideoUrl", "VideoVersion", "VideoUrlImpl");
        if (video == null) video = findUrlByFragment(model, 6, "VideoUrl", "VideoVersion");
        if (video == null) video = findUrlByFragment(callback, 6, "VideoUrl", "VideoVersion");
        if (video != null) {
            Log.i(TAG, "video " + video);
            saveVideo(ctx, video);
            return;
        }

        // 2) photo: biggest bitmap wins (72x72 avatars are everywhere)
        Object holder = field(controller, "contentHolder");
        if (holder == null) holder = findFieldValue(controller, "contentHolder", 2);
        ImageView best = null;
        Bitmap bestBm = null;
        if (holder != null) {
            for (ImageView iv : collectImageViews(holder, 4)) {
                Bitmap bm = bitmapOf(iv);
                if (bm == null) continue;
                if (bestBm == null || area(bm) > area(bestBm)) {
                    best = iv;
                    bestBm = bm;
                }
            }
        }
        if (bestBm == null) {
            for (ImageView iv : collectImageViews(controller, 4)) {
                Bitmap bm = bitmapOf(iv);
                if (bm == null) continue;
                if (bestBm == null || area(bm) > area(bestBm)) {
                    best = iv;
                    bestBm = bm;
                }
            }
        }
        if (bestBm == null) {
            Log.w(TAG, "no bitmap found");
            if (!dumpedGraph) {
                dumpedGraph = true;
                dump(holder, "contentHolder");
                dump(controller, "controller");
            }
            return;
        }
        Log.i(TAG, "photo " + bestBm.getWidth() + "x" + bestBm.getHeight()
                + " from " + name(best) + " view=" + best.getWidth() + "x" + best.getHeight());
        saveBitmap(ctx, bestBm);
    }

    private static int area(Bitmap bm) {
        return bm.getWidth() * bm.getHeight();
    }

    // ---------------------------------------------------------------- lookups

    private static Object findController(Object start) {
        if (start == null) return null;
        for (Field f : allFields(start.getClass())) {
            Object v;
            try {
                f.setAccessible(true);
                v = f.get(start);
            } catch (Throwable t) {
                continue;
            }
            if (v == null) continue;
            if (isController(v)) return v;
            if (!walkable(v)) continue;
            for (Field g : allFields(v.getClass())) {
                Object w;
                try {
                    g.setAccessible(true);
                    w = g.get(v);
                } catch (Throwable t) {
                    continue;
                }
                if (w != null && isController(w)) return w;
            }
        }
        return null;
    }

    private static boolean isController(Object o) {
        return o != null && o.getClass().getName().contains(CONTROLLER_HINT);
    }

    private static Context contextOf(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Context) return (Context) obj;
        for (Field f : allFields(obj.getClass())) {
            try {
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v instanceof Context) return (Context) v;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object field(Object obj, String name) {
        if (obj == null) return null;
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException nsf) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static Object findFieldValue(Object root, String nameContains, int depth) {
        Deque<Object> q = new ArrayDeque<Object>();
        q.add(root);
        int level = 0;
        while (!q.isEmpty() && level <= depth) {
            int n = q.size();
            for (int i = 0; i < n; i++) {
                Object o = q.poll();
                for (Field f : allFields(o.getClass())) {
                    Object v;
                    try {
                        f.setAccessible(true);
                        v = f.get(o);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (f.getName().contains(nameContains) && v != null) return v;
                    if (v != null && walkable(v)) q.add(v);
                }
            }
            level++;
        }
        return null;
    }

    /** Every ImageView reachable within depth, biggest-first ordering done by the caller. */
    private static List<ImageView> collectImageViews(Object root, int depth) {
        List<ImageView> out = new ArrayList<ImageView>();
        if (root == null) return out;
        Map<Object, Boolean> seen = new IdentityHashMap<Object, Boolean>();
        Deque<Object> q = new ArrayDeque<Object>();
        q.add(root);
        seen.put(root, Boolean.TRUE);
        int level = 0;
        while (!q.isEmpty() && level <= depth) {
            int n = q.size();
            for (int i = 0; i < n; i++) {
                Object o = q.poll();
                if (o instanceof ImageView) {
                    ImageView iv = (ImageView) o;
                    if (isNoise(iv)) continue;
                    out.add(iv);
                    continue;
                }
                for (Field f : allFields(o.getClass())) {
                    Object v;
                    try {
                        f.setAccessible(true);
                        v = f.get(o);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (v == null || seen.containsKey(v)) continue;
                    if (!walkable(v)) continue;
                    seen.put(v, Boolean.TRUE);
                    q.add(v);
                }
            }
            level++;
        }
        return out;
    }

    private static boolean isNoise(View v) {
        String cn = v.getClass().getName();
        if (cn.contains("CircularImageView")) return true;
        if (cn.contains("IgSimpleAvatar")) return true;
        return false;
    }

    private static Bitmap bitmapOf(ImageView iv) {
        Drawable d = iv.getDrawable();
        if (d instanceof BitmapDrawable) {
            Bitmap bm = ((BitmapDrawable) d).getBitmap();
            if (bm.isRecycled() || bm.getWidth() < MIN_SIDE || bm.getHeight() < MIN_SIDE) return null;
            return bm;
        }
        int w = iv.getWidth(), h = iv.getHeight();
        if (w < MIN_SIDE || h < MIN_SIDE) return null;
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        iv.draw(new Canvas(bm));
        return bm;
    }

    /** BFS for an object whose class name contains one of the fragments, then pull an http url out of it. */
    private static String findUrlByFragment(Object root, int maxDepth, String... fragments) {
        if (root == null) return null;
        Map<Object, Boolean> seen = new IdentityHashMap<Object, Boolean>();
        Deque<Object> q = new ArrayDeque<Object>();
        q.add(root);
        seen.put(root, Boolean.TRUE);
        int visited = 0;
        int level = 0;
        while (!q.isEmpty() && level <= maxDepth && visited < 5000) {
            int n = q.size();
            for (int i = 0; i < n && visited < 5000; i++) {
                Object o = q.poll();
                visited++;
                String cn = o.getClass().getName();
                for (String frag : fragments) {
                    if (cn.contains(frag)) {
                        String url = httpUrlIn(o);
                        if (url != null) return url;
                        break;
                    }
                }
                if (o instanceof List) {
                    for (Object item : (List<?>) o) {
                        if (item == null) continue;
                        for (String frag : fragments) {
                            if (item.getClass().getName().contains(frag)) {
                                String url = httpUrlIn(item);
                                if (url != null) return url;
                                break;
                            }
                        }
                    }
                    continue;
                }
                for (Field f : allFields(o.getClass())) {
                    Object v;
                    try {
                        f.setAccessible(true);
                        v = f.get(o);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (v == null || seen.containsKey(v)) continue;
                    if (!walkable(v)) continue;
                    seen.put(v, Boolean.TRUE);
                    q.add(v);
                }
            }
            level++;
        }
        return null;
    }

    private static String httpUrlIn(Object o) {
        for (Field f : allFields(o.getClass())) {
            if (f.getType() != String.class) continue;
            try {
                f.setAccessible(true);
                String s = (String) f.get(o);
                if (s != null && s.startsWith("http")) return s;
            } catch (Throwable ignored) {
            }
        }
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterTypes().length != 0) continue;
            if (m.getReturnType() != String.class) continue;
            String n = m.getName();
            if (!n.equals("getUrl") && !n.equals("D3o") && !n.equals("getSrc")) continue;
            try {
                String s = (String) m.invoke(o);
                if (s != null && s.startsWith("http")) return s;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean walkable(Object v) {
        if (v == null) return false;
        if (v instanceof String || v instanceof Number || v instanceof Boolean) return false;
        String cn = v.getClass().getName();
        if (cn.startsWith("java.lang") || cn.startsWith("java.util.concurrent")) return false;
        return true;
    }

    private static List<Field> allFields(Class<?> c) {
        List<Field> out = new ArrayList<Field>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (f.isSynthetic() && f.getType().isPrimitive()) continue;
                out.add(f);
            }
        }
        return out;
    }

    private static String name(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static void dump(Object o, String what) {
        if (o == null) {
            Log.w(TAG, what + " is null");
            return;
        }
        StringBuilder sb = new StringBuilder(what).append(" fields: ");
        for (Field f : allFields(o.getClass())) {
            sb.append(f.getType().getSimpleName()).append(' ').append(f.getName()).append("; ");
        }
        Log.w(TAG, sb.toString());
    }

    // ------------------------------------------------------------------ photo

    private static void saveBitmap(Context ctx, Bitmap bm) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bytes = bos.toByteArray();
        String fileName = "vo_" + System.currentTimeMillis() + ".png";
        Uri uri = insert(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileName, "image/png");
        if (uri == null) return;
        try {
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            os.write(bytes);
            os.close();
            publish(ctx, uri);
            Log.i(TAG, "saved photo " + bytes.length + "B " + bm.getWidth() + "x" + bm.getHeight() + " -> " + uri);
        } catch (Throwable t) {
            Log.e(TAG, "photo write failed: " + t, t);
            try {
                ctx.getContentResolver().delete(uri, null, null);
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ video

    private static void saveVideo(final Context ctx, final String url) {
        new Thread(new Runnable() {
            public void run() {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(180000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Instagram 455.0.0.51.72 Android");
                    conn.connect();
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        Log.e(TAG, "video HTTP " + code + " " + url);
                        return;
                    }
                    String fileName = "vo_" + System.currentTimeMillis() + ".mp4";
                    Uri uri = insert(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileName, "video/mp4");
                    if (uri == null) return;
                    InputStream in = conn.getInputStream();
                    OutputStream out = ctx.getContentResolver().openOutputStream(uri);
                    byte[] buf = new byte[1 << 16];
                    long total = 0;
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                        total += len;
                    }
                    out.flush();
                    out.close();
                    in.close();
                    publish(ctx, uri);
                    Log.i(TAG, "saved video " + total + "B -> " + uri);
                } catch (Throwable t) {
                    Log.e(TAG, "video failed: " + t, t);
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    // ------------------------------------------------------------- MediaStore

    private static Uri insert(Context ctx, Uri collection, String fileName, String mime) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, SUBDIR);
            cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
            return ctx.getContentResolver().insert(collection, cv);
        } catch (Throwable t) {
            Log.e(TAG, "insert failed: " + t, t);
            return null;
        }
    }

    private static void publish(Context ctx, Uri uri) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.IS_PENDING, 0);
            ctx.getContentResolver().update(uri, cv, null, null);
        } catch (Throwable t) {
            Log.w(TAG, "publish failed: " + t);
        }
    }
}
