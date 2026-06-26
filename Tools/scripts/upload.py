import os
import contextlib
import json
import random
from pathlib import Path
from sys import argv

from pyrogram import Client
from pyrogram.types import InputMediaDocument, LinkPreviewOptions

# Pre-posted sticker message-IDs in the metadata channel (reused, not re-posted).
STICKER_MESSAGE_IDS = list(range(47, 54))

api_id = os.environ.get("APP_ID")
api_hash = os.environ.get("APP_HASH")
artifacts_path = Path("artifacts")
test_version = argv[3] == "test" if len(argv) > 2 else None
metadata_chat_id = argv[4] if len(argv) > 3 else None

def find_apk(abi: str) -> Path:
    dirs = list(artifacts_path.glob("*"))
    for dir in dirs:
        if dir.is_dir():
            apks = list(dir.glob("*.apk"))
            for apk in apks:
                if abi in apk.name:
                    return apk

def get_commit_info():
    commit_id_raw = os.environ.get("COMMIT_ID") or "unknown"
    commit_id = commit_id_raw[:7]
    commit_url = os.environ.get("COMMIT_URL") or "https://github.com/risin42/NagramX/commits"
    commit_message = os.environ.get("COMMIT_MESSAGE") or "unknown"
    return commit_id, commit_url, commit_message

def get_caption() -> str:
    commit_id, commit_url, commit_message = get_commit_info()
    pre = "Test version." if test_version else "Release version."
    caption = f"{pre}\n\n"
    caption += f"Commit Message:\n<blockquote expandable>{commit_message}</blockquote>\n\n"
    caption += f"See commit details [{commit_id}]({commit_url})"
    return caption

def get_document() -> list["InputMediaDocument"]:
    documents = []
    abis = ["arm64-v8a"]
    for abi in abis:
        if apk := find_apk(abi):
            documents.append(
                InputMediaDocument(
                    media = str(apk),
                )
            )
    if not documents:
        documents.append(
        InputMediaDocument(
            media = str("TMessagesProj/src/main/" + "ic_launcher_nagram_block_round-playstore.png")
        ))
    base_caption = get_caption()
    if base_caption and len(base_caption) > 1024:
        base_caption = base_caption[:1020] + "..."
    ai_summary = get_ai_summary()
    if ai_summary and len(base_caption + ai_summary) > 1024:
        ai_summary = ""
    documents[-1].caption = base_caption + ai_summary
    print(documents)
    return documents

def get_metadata():
    commit_id = "<code>" + (os.environ.get("COMMIT_ID") or "unknown")[:7] + "</code>"
    commit_message = "<code>" + (os.environ.get("COMMIT_MESSAGE") or "unknown") + "</code>"
    build_timestamp = "<code>" + (os.environ.get("BUILD_TIMESTAMP") or "-1") + "</code>"
    return build_timestamp + " " + commit_id + "\n" + commit_message

def get_ai_summary():
    ai_summary = os.environ.get("AI_SUMMARY", "")
    if ai_summary:
        return "\n\n" + "<blockquote expandable>" + normalize_message(ai_summary) + "</blockquote>"
    return ""

def normalize_message(text: str) -> str:
    return (text or "").replace("\\n", "\n")

def retry(func):
    async def wrapper(*args, **kwargs):
        for _ in range(3):
            try:
                return await func(*args, **kwargs)
            except Exception as e:
                print(e)
    return wrapper

@retry
async def send_to_channel(client: "Client", cid: str):
    with contextlib.suppress(ValueError):
        cid = int(cid)
    await client.send_media_group(
        cid,
        media = get_document(),
    )

@retry
async def send_metadata(client: "Client", cid: str):
    with contextlib.suppress(ValueError):
        cid = int(cid)
    await client.send_message(
        chat_id = cid,
        text = get_metadata(),
    )

def get_changelog() -> str:
    text = os.environ.get("CHANGELOG", "").strip()
    if not text:
        text = "What's new?\n\n" + (os.environ.get("COMMIT_MESSAGE") or "Bug fixes and improvements.")
    return text

def build_manifest(sticker_id: int, apk_id: int, changelog_id: int) -> str:
    build_ts = int(os.environ.get("BUILD_TIMESTAMP") or 0)
    version_code = int(os.environ.get("VERSION_CODE") or 0)
    version_name = os.environ.get("VERSION_NAME") or "unknown"
    manifest = {
        "build_timestamp": build_ts,
        "can_not_skip": False,
        "version": f"{version_name} ({version_code})",
        "version_code": version_code,
        "sticker": sticker_id,
        "message": changelog_id,
        "document": {"arm64-v8a": apk_id},
        "url": "",
    }
    # BaseRemoteHelper strips exactly "#updateRelease" (14 chars) then parses the rest as JSON.
    return "#updateRelease " + json.dumps(manifest, separators=(",", ":"))

async def send_manifest(client: "Client", cid: str):
    with contextlib.suppress(ValueError):
        cid = int(cid)
    if int(os.environ.get("VERSION_CODE") or 0) <= 0:
        raise RuntimeError("VERSION_CODE env must be a positive integer")
    apk = find_apk("arm64-v8a")
    if apk is None:
        raise RuntimeError("arm64-v8a APK not found in artifacts/")
    sticker_id = random.choice(STICKER_MESSAGE_IDS)
    apk_msg = await client.send_document(cid, document=str(apk), caption=get_caption())
    changelog_msg = await client.send_message(cid, get_changelog())
    await client.send_message(cid, build_manifest(sticker_id, apk_msg.id, changelog_msg.id))

def get_client(bot_token: str):
    return Client(
        "helper_bot",
        api_id=api_id,
        api_hash=api_hash,
        bot_token=bot_token,
    )

async def main():
    bot_token = argv[1]
    chat_id = argv[2]
    mode = argv[3] if len(argv) > 3 else None
    client = get_client(bot_token)
    await client.start()
    if mode == "manifest":
        await send_manifest(client, chat_id)
    else:
        await send_to_channel(client, chat_id)
        if metadata_chat_id:
            await send_metadata(client, metadata_chat_id)
    await client.log_out()

if __name__ == "__main__":
    from asyncio import run
    run(main())
