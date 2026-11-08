package dev.nxnative.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Share log › Save to Downloads: a copy of a run log in the phone's Download/Banana Native/ folder through MediaStore (no permission needed), for a phone whose share
 * menu does not open or offers nothing that takes a file; the copy can then be sent from a file manager or over USB.
 */
final class LogExport {
    private LogExport() {}

    static final String FOLDER = "Download/Banana Native";

    /** Copies log into FOLDER; returns the copy's path as a file manager shows it */
    static String toDownloads(Context c, File log) throws IOException {
        ContentResolver cr = c.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, log.getName());
        v.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
        v.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri u = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new IOException("the Downloads folder refused a new file");
        try (InputStream in = Files.newInputStream(log.toPath()); OutputStream out = cr.openOutputStream(u)) {
            if (out == null) throw new IOException("cannot write " + u);
            in.transferTo(out);
        } catch (IOException | RuntimeException e) {
            cr.delete(u, null, null);
            throw e instanceof IOException ? (IOException) e : new IOException(e.toString());
        }
        v.clear();
        v.put(MediaStore.MediaColumns.IS_PENDING, 0);
        cr.update(u, v, null, null);
        return FOLDER + "/" + log.getName();   // MediaStore may add " (1)" to a name already there
    }
}
