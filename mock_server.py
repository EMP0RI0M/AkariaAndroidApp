import http.server
import socketserver
import logging

PORT = 9000

class MockAkariaHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        # Read the content length
        content_length = int(self.headers['Content-Length'])
        # Read the body
        post_data = self.rfile.read(content_length)
        
        logging.info(f"--- Received POST request from Android App ---")
        logging.info(f"Headers: {self.headers}")
        logging.info(f"Payload Size: {len(post_data)} bytes")
        
        # Send a mock action back to the Android Accessibility Service
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        mock_response = '{"action": "tap", "x": 195.0, "y": 245.0, "confidence": 0.99}'
        self.wfile.write(mock_response.encode('utf-8'))
        logging.info("--- Sent Mock Action to Android ---")

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
    with socketserver.TCPServer(("0.0.0.0", PORT), MockAkariaHandler) as httpd:
        logging.info(f"Serving Mock Akaria Backend on port {PORT}")
        httpd.serve_forever()
