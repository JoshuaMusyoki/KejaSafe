package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap

data class PdfPageImage(
    val bitmap: ImageBitmap,
    val pageCount: Int,
)

/**
 * Draws one PDF page. Phone targets already render through [renderPdfFirstPage].
 * The browser loads a page renderer and returns a bitmap the chat can show.
 */
expect suspend fun renderPdfPageForPreview(
    bytes: ByteArray,
    pageIndex: Int,
    maxWidthPx: Int,
): PdfPageImage?

internal const val HAVEN_PDF_JS_INSTALL = """
(function () {
  if (globalThis.__havenRenderPdf) return;
  var docs = {};
  function bytesOf(data) {
    var binary = atob(data);
    var out = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
    return out;
  }
  function loadLib() {
    if (globalThis.pdfjsLib) return Promise.resolve(globalThis.pdfjsLib);
    if (globalThis.__havenPdfJs) return globalThis.__havenPdfJs;
    globalThis.__havenPdfJs = new Promise(function (resolve, reject) {
      var script = document.createElement("script");
      script.src = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js";
      script.onload = function () {
        if (!globalThis.pdfjsLib) {
          reject(new Error("PDF viewer did not start"));
          return;
        }
        globalThis.pdfjsLib.GlobalWorkerOptions.workerSrc =
          "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js";
        resolve(globalThis.pdfjsLib);
      };
      script.onerror = function () { reject(new Error("Could not load the PDF viewer")); };
      document.head.appendChild(script);
    });
    return globalThis.__havenPdfJs;
  }
  function docKey(data) {
    return data.length + ":" + data.slice(0, 24) + ":" + data.slice(-16);
  }
  globalThis.__havenRenderPdf = function (id, b64, page, maxW) {
    var finish = function (payload) {
      var callback = globalThis.__havenPdfDone;
      if (callback) callback(String(id) + "\n" + payload);
    };
    loadLib().then(function (lib) {
      var key = docKey(b64);
      if (!docs[key]) docs[key] = lib.getDocument({ data: bytesOf(b64) }).promise;
      return docs[key].catch(function (err) {
        delete docs[key];
        throw err;
      });
    }).then(function (pdf) {
      var index = Math.min(Math.max(page, 0), pdf.numPages - 1);
      return pdf.getPage(index + 1).then(function (pg) {
        var base = pg.getViewport({ scale: 1 });
        var scale = Math.min(maxW / base.width, 1600 / base.height, 2);
        var viewport = pg.getViewport({ scale: scale });
        var canvas = document.createElement("canvas");
        canvas.width = Math.max(1, Math.floor(viewport.width));
        canvas.height = Math.max(1, Math.floor(viewport.height));
        return pg.render({ canvasContext: canvas.getContext("2d"), viewport: viewport }).promise.then(function () {
          var url = canvas.toDataURL("image/jpeg", 0.86);
          finish(JSON.stringify({
            pages: pdf.numPages,
            png: url.substring(url.indexOf(",") + 1)
          }));
        });
      });
    }).catch(function (err) {
      finish(JSON.stringify({ error: (err && err.message) ? err.message : "Could not open this PDF" }));
    });
  };
})();
"""
