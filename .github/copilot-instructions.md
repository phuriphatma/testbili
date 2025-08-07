<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# PDF Viewer App - Copilot Instructions

This is a modern web application built with Vite and vanilla JavaScript that provides PDF viewing capabilities with touch-friendly controls optimized for iPad usage.

## Key Features
- PDF import and viewing using PDF.js
- Touch-friendly interface with scroll and pinch zoom gestures
- Bookmark functionality with Cmd+D shortcut
- Custom bookmark naming and persistence
- Responsive design optimized for both desktop and mobile/tablet

## Architecture
- **Frontend**: Vanilla JavaScript with Vite build tool
- **PDF Rendering**: PDF.js library for cross-browser PDF support
- **Storage**: LocalStorage for bookmark persistence
- **Styling**: Modern CSS with responsive design and touch optimizations

## Development Guidelines
- Maintain touch-friendly interface design (minimum 44px touch targets)
- Ensure gesture handling works smoothly on iPads
- Keep accessibility in mind for keyboard navigation
- Use modern JavaScript features while maintaining broad browser compatibility
- Follow mobile-first responsive design principles

## File Structure
- `index.html` - Main HTML structure
- `main.js` - Core application logic and PDF handling
- `style.css` - Styling with responsive design and animations
- `vite.config.js` - Vite configuration for development and build

## Touch Gesture Support
- Single finger swipe: Navigate between pages
- Pinch zoom: Scale PDF content (0.5x to 3x)
- Touch-friendly scrolling for mobile devices
- Optimized touch-action CSS properties for smooth interactions
