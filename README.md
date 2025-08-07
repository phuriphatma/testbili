# iPad PDF Viewer

A modern, touch-optimized PDF viewer built with Vite and vanilla JavaScript, specifically designed for iPad usage with smooth pinch zoom and continuous scrolling.

![PDF Viewer Demo](https://img.shields.io/badge/Status-Ready-green)
![Vite](https://img.shields.io/badge/Vite-5.4.8-blue)
![PDF.js](https://img.shields.io/badge/PDF.js-4.7.76-red)

## ✨ Features

- **� Touch-Optimized**: Designed specifically for iPad with responsive touch controls
- **🔍 Smooth Pinch Zoom**: Real-time pinch zoom (0.5x - 3x) with no flash or spacing issues
- **📜 Continuous Scroll**: Seamless page-by-page scrolling with memory-efficient rendering
- **🔖 Smart Bookmarks**: Cmd+D shortcut with custom naming and localStorage persistence
- **⚡ High Performance**: Progressive rendering with memory management for large PDFs
- **🎨 Modern UI**: Clean, minimal interface with touch-friendly controls

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ 
- npm or yarn

### Installation

```bash
# Clone the repository
git clone https://github.com/phuriphatma/pdf-viewer.git
cd pdf-viewer

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:5173` and accessible from other devices on your network.

### Building for Production

```bash
npm run build
npm run preview
```

## 🎮 Usage

1. **Import PDF**: Click "Choose PDF File" or drag & drop a PDF file
2. **Navigate**: 
   - Scroll up/down to navigate pages
   - Use page navigation controls
   - Jump to specific page numbers
3. **Zoom**: 
   - Pinch to zoom in/out (iPad)
   - Use zoom controls (desktop)
4. **Bookmark**: 
   - Press `Cmd+D` (Mac) or `Ctrl+D` (Windows/Linux)
   - Enter custom bookmark name
   - Access bookmarks from the bookmark panel

## 🛠️ Technical Architecture

### Core Technologies
- **Frontend**: Vanilla JavaScript (ES6+)
- **Build Tool**: Vite 5.4.8
- **PDF Engine**: PDF.js 4.7.76
- **Storage**: localStorage for bookmarks
- **Styling**: Modern CSS with CSS Grid/Flexbox

### Key Features Implementation

#### Memory-Efficient Rendering
- Progressive page rendering with render queue
- Automatic unloading of distant pages to prevent memory crashes
- Smart viewport-based page management

#### Smooth Pinch Zoom
- CSS transform-based real-time zoom feedback
- High-quality re-render after gesture completion
- Zero-flash transition from transform to layout
- Precise scroll position preservation

#### Touch Gesture Support
- Optimized `touch-action` CSS properties
- Gesture conflict resolution
- Smooth scrolling with momentum

## 📁 Project Structure

```
├── index.html          # Main HTML structure
├── main.js            # Core application logic
├── style.css          # Responsive styling
├── vite.config.js     # Vite configuration
├── package.json       # Dependencies and scripts
└── public/
    ├── pdf.worker.min.js  # PDF.js worker
    └── vite.svg          # App icon
```
└── .github/
    └── copilot-instructions.md
```

## 🎮 Usage

### Basic Operations
1. **Import PDF**: Click the "Import PDF" button and select a PDF file
2. **Navigate**: 
   - Use arrow keys (← →) or swipe gestures
   - Scroll with mouse wheel on desktop
3. **Zoom**: 
   - Use pinch gestures on touch devices
   - `Ctrl/Cmd + scroll` on desktop

### Bookmarking
1. **Create Bookmark**: Press `Cmd+D` while viewing any page
2. **Name Bookmark**: Enter a custom name in the popup dialog
3. **Access Bookmarks**: Use the bookmark dropdown to jump to saved pages

### Keyboard Shortcuts
- `Cmd+D` - Create bookmark for current page
- `←` / `→` - Navigate between pages
- `Escape` - Close bookmark modal
- `Enter` - Save bookmark (when modal is open)

## ⚙️ Configuration

### Vite Configuration
The app is configured for:
- LAN access for cross-device testing
- Local PDF.js worker to avoid CORS issues
- Optimized build output

### Environment Setup
For code-server development (VS Code in browser):
```bash
./start-code-server.sh
```

## 🐛 Troubleshooting

### Common Issues

1. **PDF not loading**: Ensure PDF.js worker is properly loaded
2. **Zoom performance**: Check if hardware acceleration is enabled
3. **Memory issues**: Large PDFs (>500 pages) automatically use progressive rendering
4. **Touch gestures not working**: Verify touch-action CSS properties

### Browser Compatibility
- ✅ Safari (iOS/macOS)
- ✅ Chrome (Android/Desktop)
- ✅ Firefox (Desktop)
- ✅ Edge (Desktop)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines
- Maintain touch-friendly design (minimum 44px touch targets)
- Ensure smooth iPad gesture handling
- Keep accessibility in mind
- Follow mobile-first responsive design

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [PDF.js](https://mozilla.github.io/pdf.js/) - Mozilla's PDF rendering library
- [Vite](https://vitejs.dev/) - Fast build tool and dev server
- Design inspiration from modern mobile PDF readers

## 📊 Performance

- **Memory Usage**: Optimized for large PDFs with automatic page unloading
- **Render Performance**: ~60fps pinch zoom with CSS transforms
- **Loading Speed**: Progressive rendering reduces initial load time
- **Touch Response**: <16ms touch event handling for smooth gestures

---

Built with ❤️ using Vite and modern web technologies.
