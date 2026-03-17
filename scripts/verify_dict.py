import requests, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE  = "http://localhost:8083"
token = requests.post(f"{BASE}/api/auth/login",
                      json={"account":"wr_admin","password":"Admin@2025"}).json()["data"]["token"]
hdrs  = {"Authorization": token}

# 字典类型列表
types = requests.get(f"{BASE}/wr/dict/types", headers=hdrs).json()["data"]
print(f"字典类型数: {len(types)}")
for t in types:
    items = requests.get(f"{BASE}/wr/dict/items/{t['dictCode']}", headers=hdrs).json()["data"]
    opts  = ", ".join(f"{i['itemLabel']}={i['itemValue']}" for i in items)
    print(f"  [{t['dictCode']}] {t['dictName']}  ->  {opts}")

# 附件2 绑定了 dict_code 的叶子节点
items2 = requests.get(f"{BASE}/wr/template/items/2000000000000001", headers=hdrs).json()["data"]
dict_items = [i for i in items2 if i.get("dictCode")]
print(f"\n附件2 绑定字典的叶子节点: {len(dict_items)}")
for i in dict_items:
    print(f"  [{i['itemName']}] dictCode={i['dictCode']}")
