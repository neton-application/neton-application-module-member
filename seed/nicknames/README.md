# Nickname seed pools

陪玩机器人 / 普通用户「填昵称引导」prefill 用的词库种子文件。

风格: 浪漫 + 古典诗意 (温柔、清雅、自然、季节、月光、烟雨、玉墨)。

## 文件

- `adjectives.txt` — 形容词 (温柔/清雅/月色/晨曦 等)
- `nouns.txt` — 名词 (明月/海棠/烟雨/玉笛 等)

> 一行一词. import 时去重 (DB 列 UNIQUE), 重复 / 空行 / 空白被跳过.

## 导入

通过 admin API 上传 (import endpoint 内部 dedup):

```bash
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  http://localhost:8080/admin/system/auth/login \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')

# 导入形容词
ADJ=$(python3 -c "import json; print(json.dumps({'words':[l.strip() for l in open('adjectives.txt') if l.strip()]}))")
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "$ADJ" http://localhost:8080/admin/member/nickname-adjective/import

# 导入名词
NOUN=$(python3 -c "import json; print(json.dumps({'words':[l.strip() for l in open('nouns.txt') if l.strip()]}))")
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "$NOUN" http://localhost:8080/admin/member/nickname-noun/import
```

或在 admin web-antd 后台用「词库管理」页面 .txt 上传 (后续 NICK-6 落地).

## 组合规模

- adjectives ≈ 500
- nouns ≈ 500
- 组合 ≈ 250,000 (撞库率 0.0004%, 万分之 0.4)

实际可观风格: 「温柔月光」「清浅海棠」「微醺烟雨」「孤云玉笛」「霁月梨花」「桂魄落霞」等。
