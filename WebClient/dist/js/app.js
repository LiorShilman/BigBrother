// ===== Configuration =====
const CONFIG_KEY = 'bigbrother_server_url';
const ROLE_KEY = 'bigbrother_role';
const NAME_KEY = 'bigbrother_tracker_name';
let SERVER_URL = localStorage.getItem(CONFIG_KEY) || 'http://shilmanlior2608.ddns.net:26500';
let connection = null;
let map = null;
let markers = {};
let markerLayer = null;
let trackingWatchId = null;
let trackingInterval = null;
let lastTrackedPosition = null;

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initMap();
    initPanel();
    initConfigModal();
    initNotifyForm();
    initTracker();

    const savedRole = localStorage.getItem(ROLE_KEY);
    if (!SERVER_URL) {
        showConfigModal();
    } else if (!savedRole) {
        showRoleModal();
    } else {
        applyRole(savedRole);
    }
});

// ===== Role Selection =====
function showRoleModal() {
    document.getElementById('role-modal').classList.remove('hidden');
}

function hideRoleModal() {
    document.getElementById('role-modal').classList.add('hidden');
}

function selectRole(role) {
    localStorage.setItem(ROLE_KEY, role);
    hideRoleModal();
    applyRole(role);
}

function applyRole(role) {
    if (role === 'tracker') {
        document.getElementById('tracker-panel').classList.remove('hidden');
        document.getElementById('bottom-panel').classList.add('hidden');
        document.getElementById('btn-role-switch').title = 'Switch to Viewer';
        document.getElementById('btn-role-text').textContent = 'Viewer';
        document.getElementById('btn-role-switch').classList.remove('hidden');

        const savedName = localStorage.getItem(NAME_KEY);
        if (savedName) {
            document.getElementById('tracker-name').value = savedName;
        }

        connectToServer();
    } else {
        document.getElementById('tracker-panel').classList.add('hidden');
        document.getElementById('bottom-panel').classList.remove('hidden');
        document.getElementById('btn-role-switch').title = 'Switch to Tracker';
        document.getElementById('btn-role-text').textContent = 'Tracker';
        document.getElementById('btn-role-switch').classList.remove('hidden');

        connectToServer();
        loadMarkers();
    }
}

function switchRole() {
    const current = localStorage.getItem(ROLE_KEY);
    if (current === 'tracker') {
        stopTracking();
        selectRole('viewer');
    } else {
        selectRole('tracker');
    }
}

// ===== Theme =====
function initTheme() {
    const saved = localStorage.getItem('bigbrother_theme');
    if (saved === 'dark' || (!saved && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.setAttribute('data-theme', 'dark');
    }

    document.getElementById('btn-theme').addEventListener('click', () => {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        document.documentElement.setAttribute('data-theme', isDark ? 'light' : 'dark');
        localStorage.setItem('bigbrother_theme', isDark ? 'light' : 'dark');
    });
}

// ===== Map =====
function initMap() {
    map = L.map('map', {
        zoomControl: false,
        attributionControl: false
    }).setView([32.08, 34.78], 10);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
    }).addTo(map);

    L.control.zoom({ position: 'topright' }).addTo(map);
    L.control.attribution({ position: 'bottomleft' }).addAttribution(
        '&copy; <a href="https://openstreetmap.org">OSM</a>'
    ).addTo(map);

    markerLayer = L.layerGroup().addTo(map);
}

function createMarkerIcon(name) {
    return L.divIcon({
        className: '',
        html: `<div class="custom-marker"><div class="marker-inner"></div></div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 32],
        popupAnchor: [0, -34]
    });
}

function updateMapMarkers(markerList) {
    markerLayer.clearLayers();
    markers = {};
    const bounds = [];

    markerList.forEach(m => {
        if (!m.Lat && !m.Long) return;

        const latlng = [m.Lat, m.Long];
        bounds.push(latlng);

        const hasStreetView = m.StreetViewImage0 && m.StreetViewImage0.length > 10 && m.StreetViewImage0.includes('key=') && !m.StreetViewImage0.endsWith('key=');
        const popup = `
            <div class="marker-popup">
                <h3>${escHtml(m.Name)}</h3>
                ${m.Street ? `<div class="street-address">${escHtml(m.Street)}</div>` : ''}
                <div class="info-row"><span class="label">Phone</span><span>${escHtml(m.Telephone)}</span></div>
                <div class="info-row"><span class="label">Lat</span><span>${m.Lat.toFixed(6)}</span></div>
                <div class="info-row"><span class="label">Long</span><span>${m.Long.toFixed(6)}</span></div>
                <div class="info-row"><span class="label">Alt</span><span>${m.Alt.toFixed(1)}m</span></div>
                <div class="info-row"><span class="label">Accuracy</span><span>${m.Accuracy.toFixed(0)}m</span></div>
                <div class="info-row"><span class="label">Battery</span><span>${m.Battery.toFixed(0)}% ${m.IsBatteryPlugged ? '⚡' : ''}</span></div>
                <div class="info-row"><span class="label">Time</span><span>${escHtml(m.Time)}</span></div>
                ${hasStreetView ? `
                <div class="street-view-section">
                    <div class="sv-label">Street View</div>
                    <div class="sv-gallery">
                        <img src="${escHtml(m.StreetViewImage0)}" alt="N" loading="lazy" onclick="openStreetView(${m.Lat},${m.Long})">
                        <img src="${escHtml(m.StreetViewImage90)}" alt="E" loading="lazy" onclick="openStreetView(${m.Lat},${m.Long})">
                        <img src="${escHtml(m.StreetViewImage180)}" alt="S" loading="lazy" onclick="openStreetView(${m.Lat},${m.Long})">
                        <img src="${escHtml(m.StreetViewImage_Minus90)}" alt="W" loading="lazy" onclick="openStreetView(${m.Lat},${m.Long})">
                    </div>
                </div>` : ''}
                <div class="popup-actions">
                    <button type="button" class="popup-btn" onclick="navigateWaze(${m.Lat},${m.Long})">Waze</button>
                    <button type="button" class="popup-btn" onclick="openGoogleMaps(${m.Lat},${m.Long})">Maps</button>
                    ${m.Telephone ? `<button type="button" class="popup-btn" onclick="callDevice('${escHtml(m.Telephone)}')">Call</button>` : ''}
                </div>
            </div>
        `;

        const leafletMarker = L.marker(latlng, { icon: createMarkerIcon(m.Name) })
            .bindPopup(popup)
            .addTo(markerLayer);

        markers[m.Name] = { data: m, marker: leafletMarker };
    });

    if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
    }

    updateDevicesList(markerList);
}

// ===== Navigation & Call =====
function navigateWaze(lat, lng) {
    window.open(`https://waze.com/ul?ll=${lat},${lng}&navigate=yes`, '_blank');
}

function callDevice(tel) {
    window.open(`tel:${tel}`);
}

function openGoogleMaps(lat, lng) {
    window.open(`https://www.google.com/maps?q=${lat},${lng}`, '_blank');
}

function openStreetView(lat, lng) {
    window.open(`https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${lat},${lng}`, '_blank');
}

// ===== Devices List =====
function updateDevicesList(markerList) {
    const list = document.getElementById('devices-list');
    const count = document.getElementById('device-count');
    const select = document.getElementById('notify-device');

    count.textContent = markerList.length;

    select.innerHTML = '<option value="">Select device...</option>';
    markerList.forEach(m => {
        select.innerHTML += `<option value="${escHtml(m.Name)}">${escHtml(m.Name)}</option>`;
    });

    if (markerList.length === 0) {
        list.innerHTML = '<div class="empty-state">No devices connected</div>';
        return;
    }

    list.innerHTML = markerList.map(m => {
        const initials = m.Name.substring(0, 2).toUpperCase();
        const batteryClass = m.Battery > 50 ? 'battery-high' : m.Battery > 20 ? 'battery-mid' : 'battery-low';
        return `
            <div class="device-card" onclick="flyToDevice('${escHtml(m.Name)}')">
                <div class="device-avatar">${initials}</div>
                <div class="device-info">
                    <div class="device-name">${escHtml(m.Name)}</div>
                    <div class="device-detail">${m.Street ? escHtml(m.Street) : `${m.Lat.toFixed(5)}, ${m.Long.toFixed(5)}`} &middot; ${escHtml(m.Time)}</div>
                </div>
                <div class="device-battery ${batteryClass}">${m.Battery.toFixed(0)}%</div>
            </div>
        `;
    }).join('');
}

function flyToDevice(name) {
    if (markers[name]) {
        const m = markers[name].data;
        map.flyTo([m.Lat, m.Long], 16, { duration: 0.8 });
        markers[name].marker.openPopup();
    }
}

// ===== SignalR =====
function connectToServer() {
    if (connection) {
        try { connection.stop(); } catch(e) {}
    }

    const hubUrl = SERVER_URL.replace(/\/$/, '') + '/bigbrotherhub';

    connection = new signalR.HubConnectionBuilder()
        .withUrl(hubUrl)
        .withAutomaticReconnect([0, 2000, 5000, 10000, 30000])
        .build();

    connection.on('MarkersUpdated', (data) => {
        let markerList = data;
        if (typeof data === 'string') {
            markerList = JSON.parse(data);
        }
        // Only update map in viewer mode
        if (localStorage.getItem(ROLE_KEY) !== 'tracker') {
            updateMapMarkers(markerList);
        }
    });

    connection.on('SubscribeFinished', () => {
        console.log('Subscribed to Web group');
    });

    // HeadsUp notification for tracker mode (iPhone kids)
    connection.on('HeadsUpNotification', (title, message) => {
        showToast(`${title}: ${message}`);
        // Try to show native notification
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, { body: message, icon: 'icons/icon-192.svg' });
        }
        // Vibrate if supported
        if ('vibrate' in navigator) {
            navigator.vibrate([200, 100, 200, 100, 200]);
        }
    });

    // SOS Alert
    connection.on('SOSAlert', (name, lat, lng, street, time) => {
        console.log('SOS ALERT from', name);
        showSOSAlert(name, lat, lng, street, time);

        // Fly to location on map
        if (lat && lng) {
            map.flyTo([lat, lng], 17, { duration: 1 });
        }

        // Native notification
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification('SOS ALERT - ' + name, {
                body: street || `${lat}, ${lng}`,
                icon: 'icons/icon-192.svg',
                tag: 'sos-' + name,
                requireInteraction: true
            });
        }

        // Vibrate
        if ('vibrate' in navigator) {
            navigator.vibrate([500, 200, 500, 200, 500, 200, 500]);
        }
    });

    connection.onreconnecting(() => setBadge('connecting'));
    connection.onreconnected(() => {
        setBadge('connected');
        subscribeToGroup();
    });
    connection.onclose(() => setBadge('disconnected'));

    connection.start()
        .then(() => {
            setBadge('connected');
            subscribeToGroup();
        })
        .catch(err => {
            console.error('SignalR connection error:', err);
            setBadge('disconnected');
        });
}

function subscribeToGroup() {
    const role = localStorage.getItem(ROLE_KEY);
    if (role === 'tracker') {
        const name = localStorage.getItem(NAME_KEY) || 'iPhone';
        connection.invoke('Subscribe', name, 'Android');
    } else {
        connection.invoke('Subscribe', 'WebClient', 'Web');
    }
}

function setBadge(state) {
    const badge = document.getElementById('connection-badge');
    badge.className = 'badge ' + state;
    badge.textContent = state === 'connected' ? 'Connected' :
                        state === 'connecting' ? 'Reconnecting...' : 'Disconnected';
}

// ===== REST API =====
function loadMarkers() {
    const apiUrl = SERVER_URL.replace(/\/$/, '') + '/api/maps';
    fetch(apiUrl)
        .then(r => r.json())
        .then(data => {
            let markerList = data;
            if (typeof data === 'string') markerList = JSON.parse(data);
            updateMapMarkers(markerList);
        })
        .catch(err => console.error('Failed to load markers:', err));
}

// ===== Tracker Mode =====
function initTracker() {
    document.getElementById('btn-start-tracking').addEventListener('click', startTracking);
    document.getElementById('btn-stop-tracking').addEventListener('click', stopTracking);

    // Request notification permission for receiving HeadsUp
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

function startTracking() {
    const name = document.getElementById('tracker-name').value.trim();
    if (!name) {
        showToast('Please enter your name');
        return;
    }

    if (!('geolocation' in navigator)) {
        showToast('Geolocation not supported');
        return;
    }

    localStorage.setItem(NAME_KEY, name);

    // Re-subscribe with the name for SignalR notifications
    if (connection && connection.state === signalR.HubConnectionState.Connected) {
        connection.invoke('Subscribe', name, 'Android');
    }

    // Update UI
    document.getElementById('tracker-status').textContent = 'Tracking active';
    document.getElementById('tracker-status').className = 'tracker-status active';
    document.getElementById('btn-start-tracking').classList.add('hidden');
    document.getElementById('btn-stop-tracking').classList.remove('hidden');
    document.getElementById('tracker-name').disabled = true;

    // Start watching position
    trackingWatchId = navigator.geolocation.watchPosition(
        (pos) => {
            lastTrackedPosition = pos;
            updateTrackerDisplay(pos);
            sendTrackerLocation(name, pos);
        },
        (err) => {
            console.error('Geolocation error:', err);
            document.getElementById('tracker-status').textContent = 'Location error: ' + err.message;
            document.getElementById('tracker-status').className = 'tracker-status error';
        },
        {
            enableHighAccuracy: true,
            maximumAge: 10000,
            timeout: 30000
        }
    );

    // Also send periodically (every 30 seconds) even if position hasn't changed
    trackingInterval = setInterval(() => {
        if (lastTrackedPosition) {
            sendTrackerLocation(name, lastTrackedPosition);
        }
    }, 30000);

    showToast('Tracking started');
}

function stopTracking() {
    if (trackingWatchId !== null) {
        navigator.geolocation.clearWatch(trackingWatchId);
        trackingWatchId = null;
    }
    if (trackingInterval) {
        clearInterval(trackingInterval);
        trackingInterval = null;
    }

    document.getElementById('tracker-status').textContent = 'Tracking stopped';
    document.getElementById('tracker-status').className = 'tracker-status stopped';
    document.getElementById('btn-start-tracking').classList.remove('hidden');
    document.getElementById('btn-stop-tracking').classList.add('hidden');
    document.getElementById('tracker-name').disabled = false;

    showToast('Tracking stopped');
}

function updateTrackerDisplay(pos) {
    const lat = pos.coords.latitude.toFixed(6);
    const lng = pos.coords.longitude.toFixed(6);
    const acc = pos.coords.accuracy.toFixed(0);
    const time = new Date().toLocaleTimeString();

    document.getElementById('tracker-lat').textContent = lat;
    document.getElementById('tracker-lng').textContent = lng;
    document.getElementById('tracker-acc').textContent = acc + 'm';
    document.getElementById('tracker-time').textContent = time;

    // Show position on map
    markerLayer.clearLayers();
    const latlng = [pos.coords.latitude, pos.coords.longitude];
    L.marker(latlng, { icon: createMarkerIcon('me') })
        .bindPopup(`<b>My Location</b><br>${lat}, ${lng}<br>Accuracy: ${acc}m`)
        .addTo(markerLayer);
    L.circle(latlng, { radius: pos.coords.accuracy, color: '#1B6B4E', fillOpacity: 0.1, weight: 1 })
        .addTo(markerLayer);
    map.setView(latlng, Math.max(map.getZoom(), 15));
}

async function getBatteryLevel() {
    try {
        if ('getBattery' in navigator) {
            const battery = await navigator.getBattery();
            return { level: battery.level * 100, charging: battery.charging };
        }
    } catch (e) {}
    return { level: -1, charging: false };
}

async function sendTrackerLocation(name, pos) {
    const battery = await getBatteryLevel();
    const now = new Date();
    const time = `${String(now.getDate()).padStart(2,'0')}/${String(now.getMonth()+1).padStart(2,'0')}/${String(now.getFullYear()).slice(-2)} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`;

    const marker = {
        Name: name,
        Telephone: '',
        Battery: battery.level >= 0 ? battery.level : 0,
        IsBatteryPlugged: battery.charging,
        StreetViewImage_Minus90: '',
        StreetViewImage0: '',
        StreetViewImage90: '',
        StreetViewImage180: '',
        Street: '',
        Lat: pos.coords.latitude,
        Long: pos.coords.longitude,
        Alt: pos.coords.altitude || 0,
        Accuracy: pos.coords.accuracy,
        Time: time
    };

    const apiUrl = SERVER_URL.replace(/\/$/, '') + '/api/maps/addMarker';
    try {
        await fetch(apiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(marker)
        });
    } catch (err) {
        console.error('Failed to send location:', err);
    }
}

// ===== Send Notification =====
function initNotifyForm() {
    document.getElementById('btn-send-notify').addEventListener('click', () => {
        const device = document.getElementById('notify-device').value;
        const title = document.getElementById('notify-title').value;
        const message = document.getElementById('notify-message').value;

        if (!device || !title || !message) return;

        if (connection && connection.state === signalR.HubConnectionState.Connected) {
            connection.invoke('SendHeadsUpNotification', device, title, message)
                .then(() => {
                    document.getElementById('notify-title').value = '';
                    document.getElementById('notify-message').value = '';
                    showToast('Notification sent!');
                })
                .catch(err => showToast('Failed: ' + err.message));
        }
    });
}

// ===== Bottom Panel (mobile) =====
function initPanel() {
    const panel = document.getElementById('bottom-panel');
    const handle = document.getElementById('panel-handle');

    handle.addEventListener('click', () => {
        panel.classList.toggle('expanded');
    });

    let dragging = false;
    let startY = 0;
    handle.addEventListener('touchstart', (e) => {
        dragging = true;
        startY = e.touches[0].clientY;
        panel.style.transition = 'none';
    });

    document.addEventListener('touchmove', (e) => {
        if (!dragging) return;
        const diff = startY - e.touches[0].clientY;
        if (diff > 50) {
            panel.classList.add('expanded');
            dragging = false;
            panel.style.transition = '';
        } else if (diff < -50) {
            panel.classList.remove('expanded');
            dragging = false;
            panel.style.transition = '';
        }
    });

    document.addEventListener('touchend', () => {
        dragging = false;
        panel.style.transition = '';
    });
}

// ===== Config Modal =====
function initConfigModal() {
    document.getElementById('config-url').value = SERVER_URL;

    document.getElementById('btn-config-save').addEventListener('click', () => {
        const url = document.getElementById('config-url').value.trim();
        if (!url) return;
        SERVER_URL = url;
        localStorage.setItem(CONFIG_KEY, url);
        hideConfigModal();
        if (connection) connection.stop();
        connectToServer();
        loadMarkers();
    });
}

function showConfigModal() {
    document.getElementById('config-modal').classList.remove('hidden');
}

function hideConfigModal() {
    document.getElementById('config-modal').classList.add('hidden');
}

// ===== Toast =====
function showToast(msg) {
    const toast = document.createElement('div');
    toast.textContent = msg;
    toast.style.cssText = `
        position: fixed; bottom: 80px; left: 50%; transform: translateX(-50%);
        background: var(--on-surface); color: var(--surface);
        padding: 10px 20px; border-radius: 8px; font-size: 13px; font-weight: 600;
        z-index: 3000; opacity: 0; transition: opacity 0.3s;
    `;
    document.body.appendChild(toast);
    requestAnimationFrame(() => toast.style.opacity = '1');
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

// ===== SOS Alert =====
function showSOSAlert(name, lat, lng, street, time) {
    // Remove existing SOS overlay
    const existing = document.getElementById('sos-overlay');
    if (existing) existing.remove();

    const location = street || `${lat.toFixed(5)}, ${lng.toFixed(5)}`;

    const overlay = document.createElement('div');
    overlay.id = 'sos-overlay';
    overlay.innerHTML = `
        <div class="sos-content">
            <div class="sos-icon">!</div>
            <h2>SOS ALERT</h2>
            <div class="sos-name">${escHtml(name)}</div>
            <div class="sos-location">${escHtml(location)}</div>
            <div class="sos-time">${escHtml(time)}</div>
            <div class="sos-actions">
                <button type="button" class="popup-btn" onclick="navigateWaze(${lat},${lng});dismissSOS()">Waze</button>
                <button type="button" class="popup-btn" onclick="openGoogleMaps(${lat},${lng});dismissSOS()">Maps</button>
                <button type="button" class="popup-btn sos-dismiss" onclick="dismissSOS()">Dismiss</button>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);

    // Play alert sound using Web Audio API
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        function beep(freq, start, dur) {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.frequency.value = freq;
            gain.gain.value = 0.3;
            osc.start(ctx.currentTime + start);
            osc.stop(ctx.currentTime + start + dur);
        }
        beep(880, 0, 0.3);
        beep(880, 0.5, 0.3);
        beep(880, 1.0, 0.3);
    } catch (e) {}
}

function dismissSOS() {
    const overlay = document.getElementById('sos-overlay');
    if (overlay) {
        overlay.style.animation = 'fadeOut 0.3s';
        setTimeout(() => overlay.remove(), 300);
    }
}

// ===== Utils =====
function escHtml(s) {
    if (!s) return '';
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
