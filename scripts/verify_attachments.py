import requests, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")
token = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_admin", "password": "Admin@2025"}).json()["data"]["token"]
h = {"Authorization": token}

# 5条有附件的记录
check = [
    ("超声质控中心",        "2032189692832649218"),
    ("产科医疗质控中心",     "2032189694514565121"),
    ("省神经外科技术指导中心", "2032189696062263297"),
    ("省骨科技术指导中心",   "2032189697601572865"),
    ("省口腔正畸中心",      "2032189699224768514"),
]

for org_name, rec_id in check:
    atts = requests.get(f"{BASE}/wr/attachment/list/{rec_id}", headers=h).json().get("data", [])
    print(f"\n[{org_name}] recordId={rec_id}  附件数={len(atts)}")
    for a in atts:
        name = a.get("attachName", "")
        size = a.get("attachSize", 0)
        path = str(a.get("attachPath", ""))[:55]
        print(f"  {name}  {size}B  path={path}...")
