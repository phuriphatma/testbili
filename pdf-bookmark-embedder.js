// PDF Bookmark Embedder - Handles embedding and extracting bookmarks from PDF files
// Uses pdf-lib for PDF manipulation and maintains compatibility with PDF.js viewer

export class PDFBookmarkEmbedder {
  constructor() {
    this.pdfLib = null
    this.isLoaded = false
  }

  async initialize() {
    try {
      // Dynamically import pdf-lib
      this.pdfLib = await import('pdf-lib')
      this.isLoaded = true
      console.log('PDF bookmark embedder initialized')
      return true
    } catch (error) {
      console.error('Failed to load pdf-lib:', error)
      return false
    }
  }

  /**
   * Extract bookmarks from a PDF file
   * @param {ArrayBuffer} pdfArrayBuffer - The PDF file as ArrayBuffer
   * @returns {Promise<Array>} Array of bookmark objects
   */
  async extractBookmarks(pdfArrayBuffer) {
    if (!this.isLoaded) {
      await this.initialize()
    }

    try {
      const pdfDoc = await this.pdfLib.PDFDocument.load(pdfArrayBuffer)
      
      // Try to get custom metadata where we store bookmarks
      const customMetadata = pdfDoc.getCustomMetadata()
      
      if (customMetadata && customMetadata['PDF_VIEWER_BOOKMARKS']) {
        const bookmarksData = customMetadata['PDF_VIEWER_BOOKMARKS']
        return JSON.parse(bookmarksData)
      }

      // Also check for native PDF bookmarks/outlines
      const outline = pdfDoc.catalog.get(this.pdfLib.PDFName.of('Outlines'))
      const nativeBookmarks = []
      
      if (outline) {
        // Extract native PDF bookmarks if they exist
        // This is more complex and would require parsing the outline tree
        console.log('Native PDF bookmarks found, but complex extraction not implemented yet')
      }

      return []
    } catch (error) {
      console.error('Error extracting bookmarks:', error)
      return []
    }
  }

  /**
   * Embed bookmarks into a PDF file
   * @param {ArrayBuffer} pdfArrayBuffer - The original PDF file
   * @param {Array} bookmarks - Array of bookmark objects to embed
   * @returns {Promise<Uint8Array>} Modified PDF as Uint8Array
   */
  async embedBookmarks(pdfArrayBuffer, bookmarks) {
    if (!this.isLoaded) {
      await this.initialize()
    }

    try {
      const pdfDoc = await this.pdfLib.PDFDocument.load(pdfArrayBuffer)
      
      // Store bookmarks in custom metadata
      const bookmarksJson = JSON.stringify(bookmarks)
      pdfDoc.setCustomMetadata('PDF_VIEWER_BOOKMARKS', bookmarksJson)
      
      // Also update standard metadata
      pdfDoc.setModificationDate(new Date())
      pdfDoc.setProducer('iPad PDF Viewer - Bookmark Enhanced')
      
      // For better compatibility, also try to create native PDF bookmarks
      await this.createNativeBookmarks(pdfDoc, bookmarks)
      
      // Return the modified PDF
      const pdfBytes = await pdfDoc.save()
      return pdfBytes
    } catch (error) {
      console.error('Error embedding bookmarks:', error)
      throw error
    }
  }

  /**
   * Create native PDF bookmarks (outline entries)
   * @param {PDFDocument} pdfDoc - The PDF document
   * @param {Array} bookmarks - Bookmarks to convert to native format
   */
  async createNativeBookmarks(pdfDoc, bookmarks) {
    try {
      // Clear existing outlines if any
      const catalog = pdfDoc.catalog
      
      if (bookmarks.length === 0) return

      // Create outline dictionary
      const outlineDict = pdfDoc.context.obj({
        Type: this.pdfLib.PDFName.of('Outlines'),
        Count: bookmarks.length
      })

      let firstOutline = null
      let lastOutline = null
      let prevOutline = null

      // Create bookmark entries
      for (let i = 0; i < bookmarks.length; i++) {
        const bookmark = bookmarks[i]
        const page = pdfDoc.getPage(bookmark.page - 1) // Convert to 0-based index
        
        const outlineEntry = pdfDoc.context.obj({
          Title: this.pdfLib.PDFString.of(bookmark.name),
          Parent: outlineDict,
          Dest: [page.ref, this.pdfLib.PDFName.of('XYZ'), null, null, null]
        })

        if (prevOutline) {
          prevOutline.set(this.pdfLib.PDFName.of('Next'), outlineEntry)
          outlineEntry.set(this.pdfLib.PDFName.of('Prev'), prevOutline)
        } else {
          firstOutline = outlineEntry
        }

        lastOutline = outlineEntry
        prevOutline = outlineEntry
      }

      // Set first and last references
      if (firstOutline && lastOutline) {
        outlineDict.set(this.pdfLib.PDFName.of('First'), firstOutline)
        outlineDict.set(this.pdfLib.PDFName.of('Last'), lastOutline)
        
        // Add outline to document catalog
        catalog.set(this.pdfLib.PDFName.of('Outlines'), outlineDict)
      }

      console.log(`Created ${bookmarks.length} native PDF bookmarks`)
    } catch (error) {
      console.warn('Failed to create native bookmarks:', error)
      // Don't throw - embedding in metadata is still successful
    }
  }

  /**
   * Check if a PDF has embedded bookmarks
   * @param {ArrayBuffer} pdfArrayBuffer - The PDF file as ArrayBuffer
   * @returns {Promise<boolean>} True if bookmarks are found
   */
  async hasEmbeddedBookmarks(pdfArrayBuffer) {
    const bookmarks = await this.extractBookmarks(pdfArrayBuffer)
    return bookmarks.length > 0
  }

  /**
   * Get bookmark count from PDF
   * @param {ArrayBuffer} pdfArrayBuffer - The PDF file as ArrayBuffer
   * @returns {Promise<number>} Number of bookmarks
   */
  async getBookmarkCount(pdfArrayBuffer) {
    const bookmarks = await this.extractBookmarks(pdfArrayBuffer)
    return bookmarks.length
  }

  /**
   * Remove all bookmarks from a PDF
   * @param {ArrayBuffer} pdfArrayBuffer - The original PDF file
   * @returns {Promise<Uint8Array>} PDF with bookmarks removed
   */
  async removeBookmarks(pdfArrayBuffer) {
    if (!this.isLoaded) {
      await this.initialize()
    }

    try {
      const pdfDoc = await this.pdfLib.PDFDocument.load(pdfArrayBuffer)
      
      // Remove custom metadata
      pdfDoc.setCustomMetadata('PDF_VIEWER_BOOKMARKS', '')
      
      // Remove native outline
      const catalog = pdfDoc.catalog
      catalog.delete(this.pdfLib.PDFName.of('Outlines'))
      
      const pdfBytes = await pdfDoc.save()
      return pdfBytes
    } catch (error) {
      console.error('Error removing bookmarks:', error)
      throw error
    }
  }
}

// Create a global instance
export const pdfBookmarkEmbedder = new PDFBookmarkEmbedder()
