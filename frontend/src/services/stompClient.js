import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

class CollabXStompClient {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.isConnected = false;
    this.pendingSubscriptions = [];
    this.onConnectCallbacks = new Set();
    this.onDisconnectCallbacks = new Set();
  }

  connect(wsUrl) {
    if (this.client && (this.client.active || this.isConnected)) {
      return this;
    }

    const endpoint = wsUrl || (typeof window !== "undefined" ? window.location.origin + "/ws" : "http://localhost:8080/ws");

    this.client = new Client({
      webSocketFactory: () => new SockJS(endpoint),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: (frame) => {
        this.isConnected = true;
        console.log("[STOMP] Connected to CollabX WebSocket:", frame);

        // Process pending subscriptions
        const pending = [...this.pendingSubscriptions];
        this.pendingSubscriptions = [];
        pending.forEach(({ topic, callback }) => {
          this.subscribe(topic, callback);
        });

        this.onConnectCallbacks.forEach((cb) => {
          try { cb(); } catch (e) { console.error(e); }
        });
      },
      onDisconnect: () => {
        this.isConnected = false;
        console.log("[STOMP] Disconnected from CollabX WebSocket");
        this.subscriptions.clear();
        this.onDisconnectCallbacks.forEach((cb) => {
          try { cb(); } catch (e) { console.error(e); }
        });
      },
      onStompError: (frame) => {
        console.error("[STOMP] Broker reported error: " + frame.headers["message"]);
        console.error("[STOMP] Additional details: " + frame.body);
      },
      onWebSocketClose: () => {
        this.isConnected = false;
      },
    });

    this.client.activate();
    return this;
  }

  onConnected(callback) {
    if (this.isConnected) {
      callback();
    }
    this.onConnectCallbacks.add(callback);
    return () => this.onConnectCallbacks.delete(callback);
  }

  onDisconnected(callback) {
    this.onDisconnectCallbacks.add(callback);
    return () => this.onDisconnectCallbacks.delete(callback);
  }

  subscribe(topic, callback) {
    if (!this.isConnected || !this.client) {
      this.pendingSubscriptions.push({ topic, callback });
      return () => {
        this.pendingSubscriptions = this.pendingSubscriptions.filter(
          (s) => !(s.topic === topic && s.callback === callback)
        );
      };
    }

    // If already subscribed to topic, unsubscribe first
    if (this.subscriptions.has(topic)) {
      try {
        this.subscriptions.get(topic).unsubscribe();
      } catch (err) {
        console.warn(err);
      }
    }

    const sub = this.client.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body);
        callback(payload);
      } catch {
        callback(message.body);
      }
    });

    this.subscriptions.set(topic, sub);

    return () => {
      try {
        sub.unsubscribe();
        this.subscriptions.delete(topic);
      } catch (e) {
        console.warn("Error unsubscribing from " + topic, e);
      }
    };
  }

  publish(destination, body = {}) {
    if (!this.client || !this.isConnected) {
      console.warn("[STOMP] Cannot publish while not connected:", destination);
      return;
    }

    this.client.publish({
      destination,
      body: typeof body === "string" ? body : JSON.stringify(body),
    });
  }

  disconnect() {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (e) {
        console.warn("Error during STOMP disconnect:", e);
      }
      this.isConnected = false;
      this.subscriptions.clear();
      this.pendingSubscriptions = [];
      this.client = null;
    }
  }
}

export const stompClient = new CollabXStompClient();
