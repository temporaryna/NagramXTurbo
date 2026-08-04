# Cloud Bot API uploader (HTTPS to api.telegram.org). Replaces the Pyrogram/MTProto
# uploader — MTProto sessions from GitHub Actions IPs tripped anti-flood and got
# freshly-created bots banned. No api_id/api_hash, no third-party deps (stdlib only).
import html
import json
import os
import random
import time
import urllib.error
import urllib.request
from sys import argv

# Pre-posted sticker message-IDs in the metadata channel (reused, not re-posted).
STICKER_MESSAGE_IDS = list(range(47, 54))

API_BASE = "https://api.telegram.org/bot"


def chat_id_arg(cid):
    try:
        return int(cid)
    except (TypeError, ValueError):
        return cid


def bot_api(method, token, **params):
    url = API_BASE + token + "/" + method
    payload = json.dumps(params).encode("utf-8")
    last_error = None
    for attempt in range(3):
        try:
            req = urllib.request.Request(
                url, data=payload, headers={"Content-Type": "application/json"}
            )
            with urllib.request.urlopen(req, timeout=60) as resp:
                result = json.loads(resp.read().decode("utf-8"))
            if result.get("ok"):
                return result["result"]
            retry_after = (result.get("parameters") or {}).get("retry_after")
            if retry_after and attempt < 2:
                time.sleep(int(retry_after) + 1)
                continue
            raise RuntimeError(f"Telegram API error: {result}")
        except (urllib.error.URLError, TimeoutError, ConnectionError, OSError, ValueError, KeyError) as e:
            last_error = e
            if attempt < 2:
                time.sleep(3 * (attempt + 1))
                continue
            raise
    raise RuntimeError(f"Telegram API failed after retries: {last_error}")


def send_message(token, chat_id, text, disable_preview=True):
    return bot_api(
        "sendMessage",
        token,
        chat_id=chat_id_arg(chat_id),
        text=text,
        parse_mode="HTML",
        disable_web_page_preview=disable_preview,
    )


def get_commit_info():
    commit_id = (os.environ.get("COMMIT_ID") or "unknown")[:7]
    commit_url = os.environ.get("COMMIT_URL") or "https://github.com/temporaryna/NagramXTurbo/commits"
    commit_message = os.environ.get("COMMIT_MESSAGE") or "unknown"
    return commit_id, commit_url, commit_message


def normalize_message(text):
    return (text or "").replace("\\n", "\n")


def get_ai_summary():
    ai_summary = os.environ.get("AI_SUMMARY", "")
    if ai_summary:
        return "\n\n<blockquote expandable>" + html.escape(normalize_message(ai_summary)) + "</blockquote>"
    return ""


def format_apk_download_label(arch, version_name):
    label = "Download APK (" + arch + ")"
    if version_name:
        label += " v" + version_name
    return label


def get_caption(test_version):
    commit_id, commit_url, _ = get_commit_info()
    version_name = os.environ.get("VERSION_NAME", "")
    label = "Dev version." if test_version else "Release version."
    if version_name:
        label += " v" + version_name
    caption = html.escape(label) + "\n\n"
    caption += "<blockquote expandable>" + html.escape(normalize_message(get_changelog()), quote=False) + "</blockquote>\n\n"
    release_url = os.environ.get("RELEASE_URL", "")
    release_url_armv7 = os.environ.get("RELEASE_URL_ARMEABI_V7A", "")
    if release_url:
        apk_label = format_apk_download_label("64-bit, arm64-v8a", version_name)
        caption += '<a href="' + html.escape(release_url, quote=False) + '">' + html.escape(apk_label) + '</a>\n\n'
    if release_url_armv7:
        apk_label = format_apk_download_label("32-bit, armeabi-v7a", version_name)
        caption += '<a href="' + html.escape(release_url_armv7, quote=False) + '">' + html.escape(apk_label) + '</a>\n\n'
    caption += 'See commit details <a href="' + html.escape(commit_url, quote=False) + '">' + html.escape(commit_id) + "</a>"
    caption += get_ai_summary()
    return caption


def get_metadata():
    commit_id = "<code>" + html.escape((os.environ.get("COMMIT_ID") or "unknown")[:7]) + "</code>"
    commit_message = "<code>" + html.escape(os.environ.get("COMMIT_MESSAGE") or "unknown") + "</code>"
    build_timestamp = "<code>" + html.escape(os.environ.get("BUILD_TIMESTAMP") or "-1") + "</code>"
    return build_timestamp + " " + commit_id + "\n" + commit_message


def get_changelog():
    text = os.environ.get("CHANGELOG", "").strip()
    if not text:
        text = "What's new?\n\n" + (os.environ.get("COMMIT_MESSAGE") or "Bug fixes and improvements.")
    return text


def build_manifest(sticker_id, changelog_id):
    build_ts = int(os.environ.get("BUILD_TIMESTAMP") or 0)
    version_code = int(os.environ.get("VERSION_CODE") or 0)
    version_name = os.environ.get("VERSION_NAME") or "unknown"
    release_url = os.environ.get("RELEASE_URL") or ""
    release_url_armv7 = os.environ.get("RELEASE_URL_ARMEABI_V7A") or ""
    manifest = {
        "build_timestamp": build_ts,
        "can_not_skip": False,
        "version": f"{version_name} ({version_code})",
        "version_code": version_code,
        "sticker": sticker_id,
        "message": changelog_id,
        # APK is served via `url`. Point `document` at a real channel message (the
        # changelog) so older clients — which still request document in getMessages
        # — don't ask for id=0 and error out. Newer clients skip document when url is set.
        "document": {"arm64-v8a": changelog_id},
        "url": release_url,
        "url_armeabi_v7a": release_url_armv7,
    }
    return json.dumps(manifest, indent=4)


def send_manifest(token, chat_id):
    if int(os.environ.get("VERSION_CODE") or 0) <= 0:
        raise RuntimeError("VERSION_CODE env must be a positive integer")
    sticker_id = random.choice(STICKER_MESSAGE_IDS)
    changelog = send_message(token, chat_id, html.escape(get_changelog(), quote=False))
    changelog_id = changelog["message_id"]
    manifest = build_manifest(sticker_id, changelog_id)
    send_message(token, chat_id, "#updateRelease\n<pre>" + html.escape(manifest, quote=False) + "</pre>")


def send_to_channel(token, chat_id, test_version):
    send_message(token, chat_id, get_caption(test_version), disable_preview=False)


def send_metadata(token, chat_id):
    send_message(token, chat_id, get_metadata())


def main():
    token = argv[1]
    chat_id = argv[2]
    mode = argv[3] if len(argv) > 3 else None
    metadata_chat_id = argv[4] if len(argv) > 4 else None
    if mode == "manifest":
        send_manifest(token, chat_id)
    else:
        send_to_channel(token, chat_id, mode == "test")
        if metadata_chat_id:
            send_metadata(token, metadata_chat_id)


if __name__ == "__main__":
    main()
