import admin from 'firebase-admin'
import path from 'path'
import {createSchools} from './scraper.js'
import serviceAccount from '../serviceAccountCredentials.json'
// Initialize Firebase
admin.initializeApp({
    databaseURL: 'https://classy-c0f66.firebaseio.com',
    credential: admin.credential.cert(serviceAccount)
})

const db = admin.database()
const ref = db.ref('/')

async function createTestKey(){
  const testRef = ref.child('users')
  await testRef.set({'uidexample': ['class1 map', 'class2 map']})
  console.log('done test key')
}
async function populateFirebase() {
  const schoolsRef = ref.child('schools')
  try {
    const json = await createSchools()
    await schoolsRef.set(json)
    console.log('populateFirebase done')
    process.exit()
  } catch (e) {
    console.error('ERRORRRR')
    console.error(e)
  }
}

async function dropFirebase(){
  const schoolsRef = ref.child('schools')
  try {
    await schoolsRef.set({})
    console.log('dropFirebase done')
    process.exit()
  } catch (e) {
    console.log('ERRORRRR')
    console.error(e)
  }
}

function startScraping(){
  setInterval(populateFirebase(), 1000 * 60 * 60)
}
const args = process.argv;
if(args.length <= 2) console.log('Pass in either --d or --p as 3rd argument')
else if(args.length === 3 && args[2] === '--d') dropFirebase()
else if(args.length === 3 && args[2] === '--p') populateFirebase()
else if(args.length === 3 && args[2] === '--r') startScraping()
else console.log('Pass in 3 arguments')
