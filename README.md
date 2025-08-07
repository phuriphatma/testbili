# PDF Viewer App

A modern, touch-friendly PDF viewer web application built with Vite and vanilla JavaScript. Optimized for iPad usage with gesture support and bookmark functionality.

## ✨ Features

- **📁 Easy PDF Import**: Click to import PDF files with drag-and-drop support
- **👆 Touch Gestures**: 
  - Swipe left/right to navigate pages
  - Pinch to zoom (0.5x to 3x scale)
  - Smooth scrolling optimized for iPads
- **🔖 Smart Bookmarking**: 
  - Press `Cmd+D` to bookmark current page
  - Custom bookmark names with instant save
  - Persistent bookmarks stored locally
- **📱 Responsive Design**: Works seamlessly on desktop, tablet, and mobile
- **⚡ Fast Performance**: Built with Vite for optimal loading and performance

## 🚀 Getting Started

### Prerequisites
- Node.js (v14 or higher)
- npm or yarn

### Installation

1. Clone the repository:
```bash
git clone <your-repo-url>
cd pdf-viewer-app
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

4. Open your browser and navigate to `http://localhost:3000`

## 🛠️ Development

### Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build for production
- `npm run preview` - Preview production build locally

### Project Structure

```
pdf-viewer-app/
├── index.html          # Main HTML file
├── main.js             # Core application logic
├── style.css           # Styling and responsive design
├── vite.config.js      # Vite configuration
├── package.json        # Dependencies and scripts
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

## 🔧 Technical Details

### Dependencies
- **PDF.js**: Cross-browser PDF rendering
- **Vite**: Fast build tool and development server

### Browser Support
- Modern browsers with ES2015+ support
- Optimized for Safari on iPad
- Touch gesture support for mobile devices

### Storage
- Bookmarks are stored in browser's localStorage
- Data persists across browser sessions
- Organized by PDF filename for easy management

## 📱 iPad Optimization

The app is specifically optimized for iPad usage:
- **Touch Targets**: All interactive elements are at least 44px for easy touch
- **Gesture Recognition**: Native-feeling pinch zoom and swipe navigation
- **Performance**: Smooth 60fps rendering even with large PDF files
- **Viewport**: Properly configured for iOS Safari

## 🎨 Customization

The app uses CSS custom properties for easy theming. Key variables include:
- Color scheme (light/dark mode ready)
- Touch target sizes
- Animation timings
- Responsive breakpoints

## 🐛 Troubleshooting

### Common Issues
1. **PDF not loading**: Ensure the file is a valid PDF and under reasonable size
2. **Gestures not working**: Check that touch-action CSS is not being overridden
3. **Bookmarks not saving**: Verify localStorage is enabled in browser

### Performance Tips
- Large PDF files may take longer to load
- Pinch zoom is limited to 3x to maintain performance
- Consider optimizing PDF files for web viewing

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on multiple devices/browsers
5. Submit a pull request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

Built with ❤️ using Vite and modern web technologies.
