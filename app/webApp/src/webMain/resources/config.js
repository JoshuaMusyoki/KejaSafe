// Where the browser should call the API.
//
// Left unset during local development, so the app falls back to port 8080 on whatever
// host serves the page. deploy/deploy-web.ps1 overwrites this file in the release bundle
// with the deployed API URL, because Firebase Hosting and Cloud Run are different hosts.
//
// window.HAVENPAD_API_BASE_URL = "https://api.havenpad.co.ke";
