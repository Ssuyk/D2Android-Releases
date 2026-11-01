#define CMD_ID_HINT     38
#define ID_HINT_HOLD_MS 3000
#define ID_HINT_CHARS   64

// The app sends "hint<TAB>no scrolls text", both in its language.
static wchar_t idHintText[2][ID_HINT_CHARS];
static wchar_t idHintLine[ID_HINT_CHARS + 8];
static DWORD idHintUntil;
static int idHintHave;


static void idHintCommand(const BYTE* buf, int n) {
    int chars = (n - 8) / 2;
    if (chars <= 0 || chars >= 2 * ID_HINT_CHARS) return;
    wchar_t text[2][ID_HINT_CHARS];
    int part = 0, at = 0;
    for (int i = 0; i < chars; i++) {
        wchar_t c = (wchar_t) (buf[8 + 2 * i] | buf[9 + 2 * i] << 8);
        if (c == L'\t' && part == 0) {
            text[0][at] = 0;
            part = 1;
            at = 0;
            continue;
        }
        if (at >= ID_HINT_CHARS - 1) return;
        text[part][at++] = c == L'\n' || c == 0x00FF ? L' ' : c;
    }
    text[part][at] = 0;
    if (part == 0) text[1][0] = 0;
    if (!idHintHave || memcmp(text, idHintText, sizeof(text))) logLine("id hint: %d characters", chars);
    memcpy(idHintText, text, sizeof(text));
    idHintHave = 1;
    idHintUntil = GetTickCount() + ID_HINT_HOLD_MS;
}


static int idHintWanted(DWORD now) {
    return idHintHave && (int) (now - idHintUntil) < 0;
}



static int idHintCanIdentify(void* player) {
    void* inventory = player ? RDP(player, U_INVENTORY) : NULL;
    int guard = 0;
    for (void* item = readable(inventory, 0x24) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD8(data, IDATA_LOCATION) == 0 && RD8(data, IDATA_NODEPAGE) == 1) {
            const char* code = itemCode(item);
            if (codeIs(code, "isc") || (codeIs(code, "ibk") && fGetUnitStat(item, STAT_QUANTITY, 0) > 0)) return 1;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return 0;
}


static int idHintLineAdd(void) {
    if (!idHintWanted(GetTickCount()) || !hoverText || !hoverText[0] || RD32(client, RVA_HoverGrid) != 1) return 0;
    void* item = RDP(client, RVA_HoverItem);
    if (!readable(item, 0x18) || RD32(item, U_TYPE) != UNIT_ITEM) return 0;
    void* data = RDP(item, U_DATA);
    if (!readable(data, 0x70) || (RD32(data, IDATA_FLAGS) & IFLAG_IDENTIFIED)) return 0;
    int can = idHintCanIdentify(fGetPlayerUnit());
    if (!idHintText[can ? 0 : 1][0]) return 0;
    int n = wsprintfW(idHintLine, can ? L"\x00FF" L"c3%s\n" : L"\x00FF" L"c1%s\n", idHintText[can ? 0 : 1]);
    int length = lstrlenW(hoverText);
    if (n <= 0 || length + n >= HOVER_CHARS) return 0;
    memmove(hoverText + n, hoverText, (length + 1) * sizeof(wchar_t));
    memcpy(hoverText, idHintLine, n * sizeof(wchar_t));
    return n;
}


static void idHintLineRemove(int added) {
    if (added <= 0 || memcmp(hoverText, idHintLine, added * sizeof(wchar_t))) return;
    memmove(hoverText, hoverText + added, (lstrlenW(hoverText + added) + 1) * sizeof(wchar_t));
}
