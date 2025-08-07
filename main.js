import './style.css'
import * as pdfjsLib from 'pdfjs-dist'

// Configure PDF.js worker - use local worker file
pdfjsLib.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.js'

class PDFViewer {
  constructor() {
    console.log('PDFViewer constructor called')
    this.pdf = null
    this.currentPage = 1
    this.totalPages = 0
    this.scale = 1.3 // Moderate scale for memory balance
    this.bookmarks = JSON.parse(localStorage.getItem('pdfBookmarks') || '[]')
    this.currentPdfName = ''
    this.isZooming = false // Track zoom state to prevent scroll interference
    
    // Progressive rendering
    this.pageCanvases = {}
    this.pageRenderQueue = []
    this.isRendering = false
    
    console.log('Initializing elements...')
    this.initializeElements()
    console.log('Binding events...')
    this.bindEvents()
    console.log('Loading bookmarks...')
    this.loadBookmarks()
    console.log('PDFViewer initialization complete')
  }

  initializeElements() {
    console.log('Getting DOM elements...')
    this.elements = {
      importBtn: document.getElementById('importBtn'),
      pdfInput: document.getElementById('pdfInput'),
      debugBtn: document.getElementById('debugBtn'),
      pdfViewer: document.getElementById('pdfViewer'),
      pdfContainer: document.getElementById('pdfContainer'),
      pagesContainer: document.getElementById('pagesContainer'),
      welcomeMessage: document.getElementById('welcomeMessage'),
      pageInfo: document.getElementById('pageInfo'),
      currentPage: document.getElementById('currentPage'),
      totalPages: document.getElementById('totalPages'),
      bookmarkControls: document.getElementById('bookmarkControls'),
      bookmarkBtn: document.getElementById('bookmarkBtn'),
      bookmarkList: document.getElementById('bookmarkList'),
      bookmarkModal: document.getElementById('bookmarkModal'),
      bookmarkName: document.getElementById('bookmarkName'),
      bookmarkPageNum: document.getElementById('bookmarkPageNum'),
      saveBookmark: document.getElementById('saveBookmark'),
      cancelBookmark: document.getElementById('cancelBookmark')
    }
    
    // Debug: Check if critical elements exist
    console.log('Import button found:', !!this.elements.importBtn)
    console.log('PDF input found:', !!this.elements.pdfInput)
    console.log('PDF container found:', !!this.elements.pdfContainer)
    
    // Log missing elements
    Object.entries(this.elements).forEach(([key, element]) => {
      if (!element) {
        console.error(`Missing element: ${key}`)
      }
    })
  }

  bindEvents() {
    // File import
    if (this.elements.importBtn && this.elements.pdfInput) {
      console.log('Binding import button click event')
      this.elements.importBtn.addEventListener('click', () => {
        console.log('Import button clicked')
        try {
          this.elements.pdfInput.click()
        } catch (error) {
          console.error('Error clicking file input:', error)
          // Fallback: try to focus and trigger manually
          this.elements.pdfInput.focus()
          setTimeout(() => {
            this.elements.pdfInput.click()
          }, 100)
        }
      })
      
      console.log('Binding file input change event')
      this.elements.pdfInput.addEventListener('change', (e) => {
        console.log('File input changed:', e.target.files)
        if (e.target.files && e.target.files[0]) {
          this.handleFileImport(e.target.files[0])
        }
      })
    } else {
      console.error('Import button or file input not found!')
    }

    // Debug button
    if (this.elements.debugBtn) {
      this.elements.debugBtn.addEventListener('click', () => {
        console.log('Debug button clicked!')
        this.runDiagnostics()
      })
    }

    // Container events for smooth scrolling
    this.bindContainerEvents()

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      if (e.metaKey && e.key === 'd') {
        e.preventDefault()
        this.showBookmarkModal()
      } else if (e.key === 'ArrowLeft') {
        this.previousPage()
      } else if (e.key === 'ArrowRight') {
        this.nextPage()
      } else if (e.key === 'Escape') {
        this.hideBookmarkModal()
      } else if (e.key === 'Enter' && this.elements.bookmarkModal.style.display !== 'none') {
        this.saveBookmark()
      }
    })

    // Bookmark controls
    this.elements.bookmarkBtn.addEventListener('click', () => {
      this.showBookmarkModal()
    })

    this.elements.bookmarkList.addEventListener('change', (e) => {
      if (e.target.value) {
        this.goToBookmark(e.target.value)
      }
    })

    // Modal controls
    this.elements.saveBookmark.addEventListener('click', () => {
      this.saveBookmark()
    })

    this.elements.cancelBookmark.addEventListener('click', () => {
      this.hideBookmarkModal()
    })

    // Close modal on backdrop click
    this.elements.bookmarkModal.addEventListener('click', (e) => {
      if (e.target === this.elements.bookmarkModal) {
        this.hideBookmarkModal()
      }
    })
  }

  bindContainerEvents() {
    const container = this.elements.pdfContainer

    if (!container) return

    // Update current page on scroll
    container.addEventListener('scroll', () => {
      this.updateCurrentPageFromScroll()
    })

    // Mouse wheel zoom
    let wheelTimeout = null
    
    container.addEventListener('wheel', (e) => {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        
        const delta = e.deltaY > 0 ? 0.9 : 1.1
        const newScale = Math.min(3.0, Math.max(0.5, this.scale * delta))
        
        // Use CSS transform for immediate smooth feedback
        this.applySmoothZoom(newScale)
        
        // Clear any pending re-render
        if (wheelTimeout) clearTimeout(wheelTimeout)
        
        // Schedule high-quality re-render after wheel stops
        wheelTimeout = setTimeout(() => {
          this.scale = newScale
          this.finalizeZoom()
        }, 300)
      }
    })

    // Touch zoom handling
    let touchStartDistance = 0
    let touchStartScale = 1
    let touchCenter = { x: 0, y: 0 }
    let zoomTimeout = null
    let lastZoomScale = this.scale

    container.addEventListener('touchstart', (e) => {
      if (e.touches.length === 2) {
        e.preventDefault()
        this.isZooming = true
        touchStartDistance = this.getDistance(e.touches[0], e.touches[1])
        touchStartScale = this.scale
        lastZoomScale = this.scale
        
        // Clear any pending zoom finalization
        if (zoomTimeout) {
          clearTimeout(zoomTimeout)
          zoomTimeout = null
        }
        
        // Calculate center point of the two touches
        touchCenter = {
          x: (e.touches[0].clientX + e.touches[1].clientX) / 2,
          y: (e.touches[0].clientY + e.touches[1].clientY) / 2
        }
      }
    })

    container.addEventListener('touchmove', (e) => {
      if (e.touches.length === 2 && this.isZooming) {
        e.preventDefault()
        const currentDistance = this.getDistance(e.touches[0], e.touches[1])
        const scaleChange = currentDistance / touchStartDistance
        const newScale = Math.min(3.0, Math.max(0.5, touchStartScale * scaleChange))
        
        // Only apply smooth zoom if scale changed significantly
        if (Math.abs(newScale - lastZoomScale) > 0.01) { // Reduced threshold for smoother feedback
          this.applySmoothZoom(newScale)
          lastZoomScale = newScale
        }
        
        // Clear any pending re-render
        if (zoomTimeout) clearTimeout(zoomTimeout)
        
        // Schedule high-quality re-render after zoom stops
        zoomTimeout = setTimeout(() => {
          if (this.isZooming) {
            this.scale = newScale
            // Add a small delay to reduce flash
            requestAnimationFrame(() => {
              this.finalizeZoom()
              this.isZooming = false
            })
          }
        }, 200) // Slightly longer delay to reduce flashing
      }
    })

    container.addEventListener('touchend', (e) => {
      if (e.touches.length < 2 && this.isZooming) {
        this.isZooming = false
        
        // Ensure final zoom is applied immediately
        if (zoomTimeout) {
          clearTimeout(zoomTimeout)
          this.scale = lastZoomScale
          this.finalizeZoom()
        }
        
        // Reset touch tracking
        touchStartDistance = 0
        lastZoomScale = this.scale
      }
    })

    container.addEventListener('touchcancel', (e) => {
      // Handle touch cancellation (e.g., when system gesture interrupts)
      if (this.isZooming) {
        this.isZooming = false
        if (zoomTimeout) {
          clearTimeout(zoomTimeout)
          this.scale = lastZoomScale
          this.finalizeZoom()
        }
      }
    })
  }

  getDistance(touch1, touch2) {
    const dx = touch1.clientX - touch2.clientX
    const dy = touch1.clientY - touch2.clientY
    return Math.sqrt(dx * dx + dy * dy)
  }

  zoom(factor, centerX = null, centerY = null) {
    const newScale = this.scale * factor
    if (newScale >= 0.5 && newScale <= 3) {
      this.scale = newScale
      this.renderCurrentPage()
    }
  }

  async handleFileImport(file) {
    if (!file) {
      console.error('No file provided')
      alert('No file selected')
      return
    }
    
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      console.error('Invalid file type:', file.type, 'Name:', file.name)
      alert('Please select a valid PDF file')
      return
    }

    console.log('Starting PDF import:', {
      name: file.name,
      size: file.size,
      type: file.type,
      lastModified: new Date(file.lastModified)
    })

    try {
      this.elements.importBtn.innerHTML = '<span class="loading"></span> Loading PDF...'
      this.elements.importBtn.disabled = true
      
      // Check PDF.js is available
      if (!pdfjsLib) {
        throw new Error('PDF.js library not loaded')
      }
      
      console.log('PDF.js version:', pdfjsLib.version)
      console.log('Worker source:', pdfjsLib.GlobalWorkerOptions.workerSrc)
      
      // Show progress to user
      console.log('Reading file as ArrayBuffer...')
      const arrayBuffer = await file.arrayBuffer()
      console.log('File read successfully, size:', arrayBuffer.byteLength, 'bytes')
      
      if (arrayBuffer.byteLength === 0) {
        throw new Error('File is empty')
      }
      
      console.log('Creating PDF document...')
      
      // Add timeout for PDF parsing
      const loadingTask = pdfjsLib.getDocument({
        data: arrayBuffer,
        verbosity: 1 // Enable verbose logging
      })
      
      // Add progress callback
      loadingTask.onProgress = (progress) => {
        console.log('Loading progress:', progress)
        if (progress.total > 0) {
          const percent = Math.round((progress.loaded / progress.total) * 100)
          this.elements.importBtn.innerHTML = `<span class="loading"></span> Loading ${percent}%...`
        }
      }
      
      console.log('Waiting for PDF to load...')
      this.pdf = await loadingTask.promise
      this.totalPages = this.pdf.numPages
      this.currentPage = 1
      this.currentPdfName = file.name.replace('.pdf', '')
      
      console.log('PDF loaded successfully:', {
        pages: this.totalPages,
        fingerprint: this.pdf.fingerprint
      })
      
      this.showPdfViewer()
      await this.loadAllPages()
      this.updatePageInfo()
      this.loadBookmarks()
      
      this.elements.importBtn.innerHTML = 'Import PDF'
      this.elements.importBtn.disabled = false
      
    } catch (error) {
      console.error('Detailed error loading PDF:', {
        name: error.name,
        message: error.message,
        stack: error.stack
      })
      
      let errorMessage = 'Error loading PDF file'
      
      if (error.name === 'InvalidPDFException') {
        errorMessage = 'Invalid PDF file. The file may be corrupted or not a valid PDF.'
      } else if (error.name === 'MissingPDFException') {
        errorMessage = 'PDF file appears to be corrupted or empty.'
      } else if (error.name === 'PasswordException') {
        errorMessage = 'This PDF is password protected. Password-protected PDFs are not supported.'
      } else if (error.message.includes('timeout')) {
        errorMessage = 'PDF loading timed out. File may be too large.'
      } else if (error.message.includes('network')) {
        errorMessage = 'Network error loading PDF. Please check your connection.'
      } else if (error.message.includes('Worker')) {
        errorMessage = 'PDF.js worker failed to load. Please refresh the page and try again.'
      } else if (error.message.includes('File is empty')) {
        errorMessage = 'The selected file appears to be empty.'
      } else {
        errorMessage = `Error loading PDF: ${error.message}`
      }
      
      alert(errorMessage)
      this.elements.importBtn.innerHTML = 'Import PDF'
      this.elements.importBtn.disabled = false
    }
  }

  showPdfViewer() {
    this.elements.welcomeMessage.style.display = 'none'
    this.elements.pdfContainer.style.display = 'block'
    this.elements.pageInfo.style.display = 'block'
    this.elements.bookmarkControls.style.display = 'flex'
  }

  async renderPage(pageNum) {
    if (this.pageCanvases[pageNum] || this.isRendering) return // Already rendered or currently rendering

    try {
      console.log(`Rendering page ${pageNum}`)
      this.isRendering = true
      
      const page = await this.pdf.getPage(pageNum)
      const viewport = page.getViewport({ scale: this.scale })

      // Create canvas container first
      let pageWrapper = document.querySelector(`[data-page-wrapper="${pageNum}"]`)
      if (!pageWrapper) {
        pageWrapper = document.createElement('div')
        pageWrapper.className = 'page-wrapper'
        pageWrapper.setAttribute('data-page-wrapper', pageNum)
        pageWrapper.style.height = `${viewport.height}px`
        pageWrapper.style.width = `${viewport.width}px`
        pageWrapper.style.margin = '10px auto 20px auto' // Consistent spacing
        pageWrapper.style.display = 'flex'
        pageWrapper.style.justifyContent = 'center'
        pageWrapper.style.alignItems = 'center'
        pageWrapper.style.backgroundColor = '#f0f0f0'
        pageWrapper.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.3)'
        pageWrapper.style.borderRadius = '4px'
        
        // Insert in correct position
        this.insertPageWrapperInOrder(pageWrapper, pageNum)
      }

      const canvas = document.createElement('canvas')
      canvas.className = 'pdf-page'
      canvas.width = viewport.width
      canvas.height = viewport.height
      canvas.setAttribute('data-page-number', pageNum)

      const ctx = canvas.getContext('2d')
      await page.render({ canvasContext: ctx, viewport }).promise

      // Clear placeholder and add canvas
      pageWrapper.innerHTML = ''
      pageWrapper.appendChild(canvas)
      
      this.pageCanvases[pageNum] = {
        canvas: canvas,
        wrapper: pageWrapper,
        rendered: true
      }

      console.log(`Page ${pageNum} rendered successfully`)
    } catch (error) {
      console.error(`Error rendering page ${pageNum}:`, error)
    } finally {
      this.isRendering = false
    }
  }

  insertPageWrapperInOrder(pageWrapper, pageNum) {
    const container = this.elements.pagesContainer
    const existingWrappers = container.querySelectorAll('[data-page-wrapper]')
    
    let inserted = false
    for (const wrapper of existingWrappers) {
      const existingPageNum = parseInt(wrapper.getAttribute('data-page-wrapper'))
      if (pageNum < existingPageNum) {
        container.insertBefore(pageWrapper, wrapper)
        inserted = true
        break
      }
    }
    
    if (!inserted) {
      container.appendChild(pageWrapper)
    }
  }

  unloadPage(pageNum) {
    if (this.pageCanvases[pageNum]) {
      const pageData = this.pageCanvases[pageNum]
      if (pageData.wrapper) {
        // Replace canvas with placeholder
        pageData.wrapper.innerHTML = `<div style="display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; background: #f9f9f9; border: 1px dashed #ccc; color: #666;">Page ${pageNum}</div>`
      }
      delete this.pageCanvases[pageNum]
      console.log(`Page ${pageNum} unloaded from memory`)
    }
  }

  getVisiblePageRange() {
    const container = this.elements.pdfContainer
    if (!container) return { start: 1, end: 1 }

    const scrollTop = container.scrollTop
    const containerHeight = container.clientHeight
    const scrollBottom = scrollTop + containerHeight

    // Find which pages are visible or near visible
    const wrappers = container.querySelectorAll('[data-page-wrapper]')
    let firstVisible = null
    let lastVisible = null

    for (const wrapper of wrappers) {
      const pageNum = parseInt(wrapper.getAttribute('data-page-wrapper'))
      const wrapperTop = wrapper.offsetTop
      const wrapperBottom = wrapperTop + wrapper.offsetHeight

      // Check if page is visible or within buffer zone
      const bufferZone = containerHeight * 0.5 // 50% buffer above and below
      if (wrapperBottom >= scrollTop - bufferZone && wrapperTop <= scrollBottom + bufferZone) {
        if (firstVisible === null) firstVisible = pageNum
        lastVisible = pageNum
      }
    }

    return {
      start: firstVisible || 1,
      end: lastVisible || 1
    }
  }

  managePageMemory() {
    const visibleRange = this.getVisiblePageRange()
    const renderBuffer = 2 // Render 2 pages ahead and behind visible range
    const unloadBuffer = 5 // Unload pages more than 5 pages away

    const renderStart = Math.max(1, visibleRange.start - renderBuffer)
    const renderEnd = Math.min(this.totalPages, visibleRange.end + renderBuffer)

    // Queue visible and nearby pages for rendering
    for (let i = renderStart; i <= renderEnd; i++) {
      if (!this.pageCanvases[i] && !this.pageRenderQueue.includes(i)) {
        this.pageRenderQueue.unshift(i) // Add to front of queue for priority
      }
    }

    // Unload distant pages to free memory
    Object.keys(this.pageCanvases).forEach(pageNum => {
      const num = parseInt(pageNum)
      if (num < visibleRange.start - unloadBuffer || num > visibleRange.end + unloadBuffer) {
        this.unloadPage(num)
      }
    })

    // Update current page
    const currentVisible = Math.floor((visibleRange.start + visibleRange.end) / 2)
    if (currentVisible !== this.currentPage) {
      this.currentPage = currentVisible
      this.updatePageInfo()
    }
  }

  renderNextPageInQueue() {
    if (this.pageRenderQueue.length === 0 || this.isRendering) return

    const nextPage = this.pageRenderQueue.shift()

    this.renderPage(nextPage).then(() => {
      // Continue processing queue with idle callback for smooth performance
      if ('requestIdleCallback' in window) {
        requestIdleCallback(() => this.renderNextPageInQueue(), { timeout: 50 })
      } else {
        setTimeout(() => this.renderNextPageInQueue(), 16) // ~60fps
      }
    }).catch(() => {
      // Continue even if one page fails
      setTimeout(() => this.renderNextPageInQueue(), 100)
    })
  }

  async loadAllPages() {
    console.log('Setting up progressive page loading...')
    this.elements.pagesContainer.innerHTML = ''
    this.pageCanvases = {}
    this.pageRenderQueue = []

    // Create all page wrappers first (for proper scrolling behavior)
    for (let i = 1; i <= this.totalPages; i++) {
      // Get a sample page to calculate dimensions
      if (i === 1) {
        const page = await this.pdf.getPage(1)
        const viewport = page.getViewport({ scale: this.scale })
        
        for (let j = 1; j <= this.totalPages; j++) {
          const pageWrapper = document.createElement('div')
          pageWrapper.className = 'page-wrapper'
          pageWrapper.setAttribute('data-page-wrapper', j)
          pageWrapper.style.height = `${viewport.height}px`
          pageWrapper.style.width = `${viewport.width}px`
          pageWrapper.style.margin = '10px auto 20px auto' // Consistent spacing
          pageWrapper.style.display = 'flex'
          pageWrapper.style.justifyContent = 'center'
          pageWrapper.style.alignItems = 'center'
          pageWrapper.style.backgroundColor = '#f0f0f0'
          pageWrapper.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.3)'
          pageWrapper.style.borderRadius = '4px'
          pageWrapper.innerHTML = `<div style="display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; background: #f9f9f9; border: 1px dashed #ccc; color: #666;">Page ${j}</div>`
          
          this.elements.pagesContainer.appendChild(pageWrapper)
        }
        break
      }
    }

    // Queue first few pages for immediate rendering
    for (let i = 1; i <= Math.min(3, this.totalPages); i++) {
      this.pageRenderQueue.push(i)
    }

    // Start progressive rendering
    this.renderNextPageInQueue()
    
    // Start memory management
    this.startMemoryManagement()
  }

  startMemoryManagement() {
    // Initial memory management
    this.managePageMemory()
    
    // Set up scroll-based memory management
    let scrollTimeout
    let isScrolling = false
    
    this.elements.pdfContainer.addEventListener('scroll', () => {
      isScrolling = true
      clearTimeout(scrollTimeout)
      
      // Immediate current page update
      this.updateCurrentPageFromScroll()
      
      // Debounced memory management and rendering
      scrollTimeout = setTimeout(() => {
        if (!this.isZooming) { // Don't interfere with zoom operations
          this.managePageMemory()
          this.renderNextPageInQueue()
        }
        isScrolling = false
      }, 100)
    })
    
    // Additional scroll event handling for scroll end detection
    this.elements.pdfContainer.addEventListener('scrollend', () => {
      if (!this.isZooming) {
        this.managePageMemory()
        this.renderNextPageInQueue()
      }
    })
  }

  updatePageInfo() {
    this.elements.currentPage.textContent = this.currentPage
    this.elements.totalPages.textContent = this.totalPages
    
    // Update scale display if it exists
    const scaleDisplay = document.getElementById('scaleDisplay')
    if (scaleDisplay) {
      scaleDisplay.textContent = `${Math.round(this.scale * 100)}%`
    }
  }

  updateCurrentPageFromScroll() {
    const range = this.getVisiblePageRange()
    const newCurrentPage = Math.floor((range.start + range.end) / 2)
    
    if (newCurrentPage !== this.currentPage) {
      this.currentPage = newCurrentPage
      this.updatePageInfo()
    }
  }

  applySmoothZoom(newScale) {
    // Apply CSS transform to the entire pages container for smooth feedback
    const scaleRatio = newScale / this.scale
    const pagesContainer = this.elements.pagesContainer
    const container = this.elements.pdfContainer
    
    if (pagesContainer) {
      // Calculate the center point of the viewport for transform origin
      const containerRect = container.getBoundingClientRect()
      const scrollTop = container.scrollTop
      const scrollLeft = container.scrollLeft
      const viewportCenterY = scrollTop + (container.clientHeight / 2)
      const viewportCenterX = scrollLeft + (container.clientWidth / 2)
      
      // Apply transform with stable origin
      pagesContainer.style.transformOrigin = `${viewportCenterX}px ${viewportCenterY}px`
      pagesContainer.style.transform = `scale(${scaleRatio})`
      
      // Prevent any layout shifts during transform
      pagesContainer.style.willChange = 'transform'
    }
    
    // Update scale display immediately
    const scaleDisplay = document.getElementById('scaleDisplay')
    if (scaleDisplay) {
      scaleDisplay.textContent = `${Math.round(newScale * 100)}%`
    }
  }

  finalizeZoom() {
    // Store current scroll state before any DOM changes
    const container = this.elements.pdfContainer
    const pagesContainer = this.elements.pagesContainer
    const currentScrollTop = container.scrollTop
    const currentScrollLeft = container.scrollLeft
    const currentPage = this.getCurrentVisiblePage()
    
    // Calculate the current center point of the viewport
    const viewportCenterY = currentScrollTop + (container.clientHeight / 2)
    const viewportCenterX = currentScrollLeft + (container.clientWidth / 2)
    
    // Get current page wrapper for reference
    const currentPageWrapper = document.querySelector(`[data-page-wrapper="${currentPage}"]`)
    let relativeY = 0.5
    let relativeX = 0.5
    
    if (currentPageWrapper) {
      const pageRect = currentPageWrapper.getBoundingClientRect()
      const containerRect = container.getBoundingClientRect()
      
      // Calculate relative position within the current page
      relativeY = Math.max(0, Math.min(1, 
        (viewportCenterY - currentPageWrapper.offsetTop) / currentPageWrapper.offsetHeight
      ))
      relativeX = Math.max(0, Math.min(1,
        (viewportCenterX - currentPageWrapper.offsetLeft) / currentPageWrapper.offsetWidth
      ))
    }
    
    // Temporarily disable scroll events to prevent interference
    const originalOverflow = container.style.overflow
    container.style.overflow = 'hidden'
    
    // Clear container transform immediately
    if (pagesContainer) {
      pagesContainer.style.transform = ''
      pagesContainer.style.transformOrigin = ''
      pagesContainer.style.willChange = 'auto'
    }
    
    // Clear any canvas transforms that might be left over
    const canvases = Object.values(this.pageCanvases)
    canvases.forEach(pageData => {
      if (pageData.canvas) {
        pageData.canvas.style.transform = ''
        pageData.canvas.style.transformOrigin = ''
      }
    })
    
    // Re-render at high quality with new scale and restore position
    this.updatePageScalesWithPosition(currentPage, relativeY, relativeX, originalOverflow)
  }

  updatePageScalesWithPosition(targetPage, relativeY, relativeX, originalOverflow) {
    // Re-render visible pages with new scale
    const visibleRange = this.getVisiblePageRange()
    const renderBuffer = 2
    
    // Clear existing canvases but keep wrappers
    Object.keys(this.pageCanvases).forEach(pageNum => {
      this.unloadPage(parseInt(pageNum))
    })
    
    // Update page wrapper dimensions
    this.pdf.getPage(1).then(page => {
      const viewport = page.getViewport({ scale: this.scale })
      
      // Update all wrapper dimensions
      const wrappers = this.elements.pagesContainer.querySelectorAll('[data-page-wrapper]')
      wrappers.forEach(wrapper => {
        wrapper.style.height = `${viewport.height}px`
        wrapper.style.width = `${viewport.width}px`
        // Maintain consistent spacing
        wrapper.style.margin = '10px auto 20px auto'
        wrapper.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.3)'
        wrapper.style.borderRadius = '4px'
      })
      
      // Immediately restore scroll position without any delay
      const targetPageWrapper = document.querySelector(`[data-page-wrapper="${targetPage}"]`)
      if (targetPageWrapper) {
        const container = this.elements.pdfContainer
        const newPageTop = targetPageWrapper.offsetTop
        const newPageLeft = targetPageWrapper.offsetLeft
        const newPageHeight = targetPageWrapper.offsetHeight
        const newPageWidth = targetPageWrapper.offsetWidth
        
        // Calculate new scroll position based on relative position
        const newScrollTop = newPageTop + (relativeY * newPageHeight) - (container.clientHeight / 2)
        const newScrollLeft = newPageLeft + (relativeX * newPageWidth) - (container.clientWidth / 2)
        
        // Set scroll position directly without animation
        container.scrollTop = Math.max(0, newScrollTop)
        container.scrollLeft = Math.max(0, newScrollLeft)
      }
      
      // Re-enable scrolling
      const container = this.elements.pdfContainer
      container.style.overflow = originalOverflow || 'auto'
      
      // Update page info with new scale
      this.updatePageInfo()
      
      // Queue visible pages for re-rendering
      const renderStart = Math.max(1, visibleRange.start - renderBuffer)
      const renderEnd = Math.min(this.totalPages, visibleRange.end + renderBuffer)
      
      this.pageRenderQueue = []
      for (let i = renderStart; i <= renderEnd; i++) {
        this.pageRenderQueue.push(i)
      }
      
      this.renderNextPageInQueue()
      
    }).catch(error => {
      console.error('Error updating page scales:', error)
      // Re-enable scrolling even on error
      const container = this.elements.pdfContainer
      container.style.overflow = originalOverflow || 'auto'
      this.updatePageInfo()
    })
  }

  updatePageScales() {
    // Store current scroll position and page info before scaling
    const container = this.elements.pdfContainer
    const currentScrollTop = container.scrollTop
    const containerHeight = container.clientHeight
    const currentPage = this.getCurrentVisiblePage()
    
    // Get current page wrapper to calculate relative position
    const currentPageWrapper = document.querySelector(`[data-page-wrapper="${currentPage}"]`)
    let relativeScrollPosition = 0.5 // Default to middle of page
    if (currentPageWrapper) {
      const pageTop = currentPageWrapper.offsetTop
      const pageHeight = currentPageWrapper.offsetHeight
      relativeScrollPosition = Math.max(0, Math.min(1, (currentScrollTop - pageTop) / pageHeight))
    }
    
    // Re-render visible pages with new scale
    const visibleRange = this.getVisiblePageRange()
    const renderBuffer = 2
    
    // Clear existing canvases but keep wrappers
    Object.keys(this.pageCanvases).forEach(pageNum => {
      this.unloadPage(parseInt(pageNum))
    })
    
    // Update page wrapper dimensions and queue for re-rendering
    this.pdf.getPage(1).then(page => {
      const viewport = page.getViewport({ scale: this.scale })
      
      // Update all wrapper dimensions
      const wrappers = this.elements.pagesContainer.querySelectorAll('[data-page-wrapper]')
      wrappers.forEach(wrapper => {
        wrapper.style.height = `${viewport.height}px`
        wrapper.style.width = `${viewport.width}px`
      })
      
      // Use requestAnimationFrame for smoother position restoration
      requestAnimationFrame(() => {
        const updatedPageWrapper = document.querySelector(`[data-page-wrapper="${currentPage}"]`)
        if (updatedPageWrapper) {
          const newPageTop = updatedPageWrapper.offsetTop
          const newPageHeight = updatedPageWrapper.offsetHeight
          const newScrollTop = newPageTop + (relativeScrollPosition * newPageHeight)
          
          // Smooth scroll to new position
          container.scrollTo({
            top: Math.max(0, newScrollTop),
            behavior: 'auto' // Use 'auto' instead of 'smooth' for immediate positioning
          })
        }
        
        // Update page info with new scale
        this.updatePageInfo()
      })
      
      // Queue visible pages for re-rendering
      const renderStart = Math.max(1, visibleRange.start - renderBuffer)
      const renderEnd = Math.min(this.totalPages, visibleRange.end + renderBuffer)
      
      this.pageRenderQueue = []
      for (let i = renderStart; i <= renderEnd; i++) {
        this.pageRenderQueue.push(i)
      }
      
      this.renderNextPageInQueue()
    }).catch(error => {
      console.error('Error updating page scales:', error)
      // Fallback: just update page info
      this.updatePageInfo()
    })
  }

  adjustScale(factor) {
    const newScale = Math.min(3.0, Math.max(0.5, this.scale * factor))
    if (Math.abs(newScale - this.scale) > 0.01) {
      this.scale = newScale
      this.updatePageScales()
      this.updatePageInfo()
    }
  }

  getCurrentVisiblePage() {
    const range = this.getVisiblePageRange()
    return Math.floor((range.start + range.end) / 2)
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.goToPage(this.currentPage - 1)
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.goToPage(this.currentPage + 1)
    }
  }

  updateZoom() {
    this.updatePageScales()
  }

  goToPage(pageNum) {
    if (pageNum >= 1 && pageNum <= this.totalPages) {
      const pageWrapper = document.querySelector(`[data-page-wrapper="${pageNum}"]`)
      if (pageWrapper) {
        this.elements.pdfContainer.scrollTo({ 
          top: pageWrapper.offsetTop, 
          behavior: 'smooth' 
        })
        
        // Ensure this page gets rendered
        if (!this.pageCanvases[pageNum] && !this.pageRenderQueue.includes(pageNum)) {
          this.pageRenderQueue.unshift(pageNum)
          this.renderNextPageInQueue()
        }
      }
    }
  }

  showBookmarkModal() {
    if (!this.pdf) {
      alert('Please load a PDF first')
      return
    }
    
    this.elements.bookmarkPageNum.textContent = this.currentPage
    this.elements.bookmarkName.value = `Page ${this.currentPage}`
    this.elements.bookmarkModal.style.display = 'flex'
    this.elements.bookmarkName.focus()
    this.elements.bookmarkName.select()
  }

  hideBookmarkModal() {
    this.elements.bookmarkModal.style.display = 'none'
  }

  saveBookmark() {
    const name = this.elements.bookmarkName.value.trim()
    if (!name) {
      alert('Please enter a bookmark name')
      return
    }

    const bookmarkKey = `${this.currentPdfName}_page_${this.currentPage}`
    
    if (!this.bookmarks[this.currentPdfName]) {
      this.bookmarks[this.currentPdfName] = {}
    }
    
    this.bookmarks[this.currentPdfName][bookmarkKey] = {
      name: name,
      page: this.currentPage,
      scale: this.scale,
      timestamp: new Date().toISOString()
    }

    localStorage.setItem('pdfBookmarks', JSON.stringify(this.bookmarks))
    this.loadBookmarks()
    this.hideBookmarkModal()
    
    // Show success feedback
    const originalText = this.elements.bookmarkBtn.textContent
    this.elements.bookmarkBtn.textContent = '✓ Bookmarked!'
    setTimeout(() => {
      this.elements.bookmarkBtn.textContent = originalText
    }, 2000)
  }

  loadBookmarks() {
    const select = this.elements.bookmarkList
    select.innerHTML = '<option value="">Select Bookmark</option>'
    
    if (this.currentPdfName && this.bookmarks[this.currentPdfName]) {
      Object.entries(this.bookmarks[this.currentPdfName]).forEach(([key, bookmark]) => {
        const option = document.createElement('option')
        option.value = key
        option.textContent = bookmark.name
        select.appendChild(option)
      })
    }
  }

  goToBookmark(bookmarkKey) {
    if (this.currentPdfName && this.bookmarks[this.currentPdfName] && this.bookmarks[this.currentPdfName][bookmarkKey]) {
      const bookmark = this.bookmarks[this.currentPdfName][bookmarkKey]
      this.scale = bookmark.scale || 1
      this.updateZoom()
      this.goToPage(bookmark.page)
    }
  }

  runDiagnostics() {
    console.log('=== PDF Viewer Diagnostics ===')
    
    // Check basic JavaScript
    console.log('✓ JavaScript is working')
    
    // Check PDF.js
    try {
      console.log('✓ PDF.js loaded, version:', pdfjsLib.version)
      console.log('✓ Worker source:', pdfjsLib.GlobalWorkerOptions.workerSrc)
      
      // Test worker accessibility
      fetch(pdfjsLib.GlobalWorkerOptions.workerSrc)
        .then(response => {
          if (response.ok) {
            console.log('✓ Worker file is accessible')
          } else {
            console.error('✗ Worker file not accessible:', response.status)
          }
        })
        .catch(error => {
          console.error('✗ Worker fetch error:', error)
        })
        
    } catch (e) {
      console.error('✗ PDF.js error:', e)
    }
    
    // Check DOM elements
    const missingElements = []
    Object.entries(this.elements).forEach(([key, element]) => {
      if (!element) {
        missingElements.push(key)
      }
    })
    
    if (missingElements.length === 0) {
      console.log('✓ All DOM elements found')
    } else {
      console.error('✗ Missing DOM elements:', missingElements)
    }
    
    // Check canvas support
    try {
      const testCanvas = document.createElement('canvas')
      const testCtx = testCanvas.getContext('2d')
      if (testCtx) {
        console.log('✓ Canvas 2D context supported')
      } else {
        console.error('✗ Canvas 2D context not supported')
      }
    } catch (e) {
      console.error('✗ Canvas error:', e)
    }
    
    // Test file API
    if (window.File && window.FileReader && window.FileList && window.Blob) {
      console.log('✓ File API supported')
    } else {
      console.error('✗ File API not fully supported')
    }
    
    // Test worker availability
    if (typeof Worker !== 'undefined') {
      console.log('✓ Web Workers supported')
    } else {
      console.error('✗ Web Workers not supported')
    }
    
    alert('Diagnostics complete! Check console for details.')
  }
}

// Initialize the PDF viewer when the page loads
document.addEventListener('DOMContentLoaded', () => {
  console.log('DOM loaded, initializing PDF viewer')
  const viewer = new PDFViewer()
  console.log('PDF viewer initialized:', viewer)
})
