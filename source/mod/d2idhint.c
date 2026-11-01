#define CMD_ID_HINT     38
#define ID_HINT_HOLD_MS 3000
#define ID_HINT_CHARS   48

static wchar_t idHintText[ID_HINT_CHARS];
static wchar_t idHintLine[ID_HINT_CHARS + 8];
static DWORD idHintUntil;
static int idHintHave;


static void idHintCommand(const BYTE* buf, int n) {
    int chars = (n - 8) / 2;
    if (chars <= 0 || chars >= ID_HINT_CHARS) return;
    wchar_t text[ID_HINT_CHARS];
    for (int i = 0; i < chars; i++) {
        wchar_t c = (wchar_t) (buf[8 + 2 * i] | buf[9 + 2 * i] << 8);
        text[i] = c == L'\n' || c == 0x00FF ? L' ' : c;
    }
    text[chars] = 0;
    if (!idHintHave || lstrcmpW(text, idHintText)) logLine("id hint: %d characters", chars);
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
    if (!readable(data, 0x70) || (RD32(data, IDATA_FLAGS) & IFLAG_IDENTIFIED) || !idHintCanIdentify(fGetPlayerUnit())) return 0;
    int n = wsprintfW(idHintLine, L"\x00FF" L"c3%s\n", idHintText);
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
