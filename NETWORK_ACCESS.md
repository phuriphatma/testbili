# Network Access Guide for iPad PDF Viewer

## 🌐 Accessing from iPad on Same Network

### Quick Setup:

1. **Start the development server** on your Mac:
   ```bash
   npm run dev
   ```

2. **Find your Mac's IP address**:
   - Your current IP: `192.168.1.182`
   - Port: `5173`

3. **Access from iPad**:
   - Open Safari on your iPad
   - Navigate to: `http://192.168.1.182:5173`
   - Bookmark this URL for easy access

### Alternative Ways to Find Your IP:

```bash
# Method 1: Using ifconfig
ifconfig | grep "inet " | grep -v 127.0.0.1

# Method 2: Using networksetup
networksetup -getinfo "Wi-Fi" | grep "IP address"

# Method 3: Using System Preferences
# System Preferences > Network > Wi-Fi > Advanced > TCP/IP
```

### Troubleshooting:

1. **Can't connect from iPad?**
   - Ensure both devices are on the same Wi-Fi network
   - Check if firewall is blocking the connection
   - Try restarting the dev server: `npm run dev`

2. **Different IP address?**
   - IP addresses can change when reconnecting to Wi-Fi
   - Always check current IP with: `ifconfig | grep "inet "`

3. **Port already in use?**
   - Vite will automatically try the next available port
   - Check the terminal output for the actual port being used

### Firewall Configuration:

If you can't connect from iPad, you may need to allow the connection:

**macOS Firewall:**
1. System Preferences > Security & Privacy > Firewall
2. Click "Firewall Options"
3. Make sure "Block all incoming connections" is unchecked
4. Add Node.js or your terminal app to allowed applications

### Network Security:

- This setup only works on your local network
- The server is not accessible from the internet
- All traffic stays within your local Wi-Fi network

## 🚀 Production Deployment

For making it accessible over the internet, consider:

1. **GitHub Pages** (static hosting)
2. **Netlify** or **Vercel** (with build process)
3. **Your own server** with proper SSL/HTTPS

## 📱 iPad Optimization

The app is already optimized for iPad with:
- Touch-friendly interface (44px minimum touch targets)
- Smooth pinch zoom gestures
- Responsive design for iPad screen sizes
- Optimized touch-action CSS properties
