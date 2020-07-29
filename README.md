# Distributed Chaos

[Postavka problema](https://docs.google.com/document/d/1gr8mdPVnaBaGB4LXXAuNHPFFgjhuWJ4sCGLSv00pLSw)

[Cela dokumentacija](https://docs.google.com/document/d/1R8uygEGYILpqh34eT_hjNgH-zQHGPYHe51d061e3e0I)


## Bootstrap API


**GET /api/bootstrap/hail** 

Pri ukljucenju prvog čvora, port i ip adresa čvoru kome treba da se javi će biti null. U suprotnom, uzima random čvorova iz liste aktivnih čvorova i vraća je čvoru.

Bootstrap mu vraća čvor kome treba da se javi:
```
{
	“ip”: “<IP adresa čvora kome treba da se javi za uključenje u mrežu>”,
	“port”: “<Port čvora kome treba da se javi za uključenje u mrežu>”
}
```

**POST /api/bootstrap/left**

Pri uklanjanju noda iz mreze, potrebno je javiti bootstrapu da vise ne nudi taj cvor kao opciju za uklucenje. Bootstap uklanja ovaj čvor iz liste aktivnih čvorova.  


Javljanje bootstrap serveru radi iskljucenja  mrezu:
```{
	“ip”: “<Adresa čvora koji salje zahtev>”,
	“port”: “<Port čvora koji šalje zahtev>”
}
```

**PUT /api/bootstrap/new**

Kada čvor uspe da se uključi u sistem javlja bootstapu da ga doda u listu aktivnih čvorova. Ova poruka se broadcastuje kroz sistem u oba smera.

```
{
	“ip”: “<Adresa čvora koji se uključio>”,
	“port”: “<Port čvora koji se uključio>”
}
```

## Node API

**GET /api/node/info**

Tražimo informacije o čvoru, tj njegovu ip adresu i port.

Response:
```
{
    "ip": "localhost",
    "port": 1000
}
```


**GET /api/node/allNodes**

Tražimo informacije o svim čvorovima u sistemu.


Response:

 ```
    [{
     "ip": "localhost",
        "port": 1000
    },
    {
        "ip": "localhost",
        "port": 1001
    },
    {
        "ip": "localhost",
        "port": 1002
    }]
```

**GET /api/node/ping/{nodeID}**


Ukoliko je nodeID u stvari ID čvora koje je zahtev poslat, pingujemo ga. U suprotnom mu tražimo da on pinguje čvor sa ID-em nodeID


Response: <true | false>

**PUT /api/node/updateNewNode**


Pri uključivanju u sistem, novi čvor pronalazi svog prethodnika i traži mu da ga doda kao svog sledbenika.


Request:
```
{
    "ip": "localhost",
    "port": 1000
}
```

**PUT /api/node/broadcastNewNode**


Pri uključivanju novog čvora u sistem ovaj zahtev će se proslediti svim čvorovima (broadcastovaće se) kako bi oni ažurirali svoje tabele.


Request:

```
{
    "ip": "localhost",
    "port": 1000
}
```


**POST /api/node/left**


Kada neki čvor napusti sistem (otkaz ili uredno gašenje) svaki čvor prima poruku o tome na ovaj endpoint (broadcastuje se )kako bi ažurirao svoje tabele. 


Request:

```
{
    "ip": "localhost",
    "port": 1000
}
```

**GET /api/node/quit**


Ukoliko želimo da uredno isključimo neki čvor iz sistema šaljemo mu zahtev na ovaj endpoint. Čvor će započeti broadcast POST /api/node/left poruke kroz sistem i isključiće se.
POST /api/node/backup

Susedni čvorovi na ovaj endpoint u body-u zahteva šalju kopije svojih podataka.


Request:

```
{	
   "jobID": "job1",
    "regionID": "-01",
    "Data": [
	{
	"X": 3.4,
	"Y": 6.7
     },
     {
	"X": 2.5,
	"Y": 5.3
     },...]
  }
```

**POST /api/node/getBackup/{jobID}/{regionID}**


Pri rekonstrukciji sistema, vraćanju rezultata i statusa želimo da dobijemo backupovane podatke za određeni posao i region sa nekog čvora.


Request:

```
{
	"ip":"localhost",
	"port": 1003
}
```
Response:

```
{
    "jobID": "job1",
    "regionID": "-01",
    "data": [
        {
            "x": 31.25,
            "y": 25.0
        },
        {
            "x": 40.625,
            "y": 25.0
        },...
    ]
}
```

## Job API

**GET /api/jobs/allJobs**


Informacije o svim poslovima u sistemu


Response:
```
[
    	[{
        "id": "job1",
        "startingPoints": [
            {
                "x": 25.0,
                "y": 25.0
            },
            {
                "x": 75.0,
                "y": 25.0
            },
            {
                "x": 50.0,
                "y": 75.0
            }
        ],
        "width": 100,
        "height": 100,
        "tracepoint": {
            "x": 25.0,
            "y": 25.0
        },
        "proportion": 0.5
    },....]
```


**PUT /api/jobs/start**


Ćvor započinje izvršavanje posla koji se nalazi u body-u


Request:
```
{
	"id":"job1",
	"startingPoints": [
		{
			"x": 25.0,
			"y": 25.0
		},
			{
			"x": 75.0,
			"y": 25.0
		},	{
			"x": 50.0,
			"y": 75.0
		}

		],
	"width": 100,
	"height": 100,
	"proportion": 0.5,
	"tracepoint": {
		"x": 25.0,
		"y": 25.0
	}
}
```

**DELETE /api/jobs/{jobID}**


Uklanjamo posao sa ID-em jobID iz celog sistema. Čvor uklanja posao iz svoje liste i broadcastuje poruku na isti endpoint kako bi svi čvorovi uradili isto.


## Result API

**GET /api/result/{jobID}**

Tražimo rezultat za ceo posao sa ID-em jobID. Od svih čvorova koji su radili na ovom poslu uzimamo rezultat za njihove regione koristeći GET /api/result/{jobID}/{regionID}

Response: Nema JSON odgovora, generiše se slika sa imenom result<jobID>.png

**GET /api/result/{jobID}/{regionID}**


Tražimo rezultat za posao sa jobID-em na regionu sa regionID-em. Čvor će preko delegatora poslati poruku svim čvorovima koji su radili ovaj posao za ovaj region da vrate svoje rezultate i backupe. S obzirom da se u backupima nalaze i podaci koje čvor računa možemo samo da uzmemo backupe pomoću zahteva 
POST /api/node/getBackup/{jobID}/{regionID} koji se prosleđuje preko delegatora.


Response: Nema JSON odgovora, generiše se slika sa imenom result<jobID>.png


## Status API

**GET /api/status**

Vraćamo status za ceo sistem, tj svaki posao u sistemu. Za svaki posao u sistemu koristimo GET /api/status/{jobID} da uzmemo njegov status, a on dalje koristi GET /api/status/{jobID}/{regionID} da uzme statuse za svoje regione.

Response:

```
{
"Id": job1,
"regions":[
    {
        "regionID": "-01",
        "nodeID": "localhost:1004",
        "numberofPoints": 70
    }, 
   {
        "regionID": "-02",
        "nodeID": "localhost:1004",
        "numberofPoints": 70
    },...]
...
}
```



**GET /api/status/{jobID}**

Vraćamo status za posao sa jobID-em tako što pronalazimo sve njegove regione i sve čvorove koji su bili zaduženi za te regione. Zatim od tih čvorova tražimo statuse za konkretne regione upotrebom GET /api/status/{jobID}/{regionID} i sve ih sumiramo. 

Response:
```
[
    {
        "regionID": "-01",
        "nodeID": "localhost:1004",
        "numberofPoints": 70
    }, 
   {
        "regionID": "-02",
        "nodeID": "localhost:1004",
        "numberofPoints": 70
    },...
]
```

**GET /api/status/{jobID}/{regionID}**


Vraćamo status za posao sa jobID-em na regionu regionID. Šaljemo preko delegatora (po Chordu) poruku čvorovima koji su radili ovaj posao za ovaj region. 


Response:
```
[
    {
        "regionID": "-02",
        "nodeID": "localhost:10d04",
        "numberofPoints": 70
    }
]
```
