import 'babel-polyfill'
import request from 'request'
import Promise from 'bluebird'

import {getListOfSchools, requestDept, requestCourse, requestSection} from './request-course.js'
//scrape departments, courses, sections, separately
const TERM = 20171;
export async function createSchools() {
  const schools = await getListOfSchools(TERM)
  console.log('got list of schools')
  //this promise will create an array of schools with departments
  const schoolsPromises = schools.map(school => {
    //this promise will find all the departments given a school, and then get all department information.
    return new Promise((resolve, reject) => {

      const departments = school.department
      if(Array.isArray(departments)) {
        //array of promises that will add a course array to a department.
        const deptArrayPromises = departments.map(department => {
          return addCoursesToDepartment(department, department.code, TERM)
        })
        // process all departments, and then update the school's departments
        // Promise.all(deptArrayPromises)
        //   .then(arrayOfDepartments => school.department = arrayOfDepartments)
        //   .then(updatedSchool => resolve(updatedSchool))
        //   .catch(err => reject(err))
        const array = []
        Promise.each(deptArrayPromises, dept => {
          array.push(dept)
        }).then(x => {
          school.department = array
          resolve(school)
        }).catch(err => reject(err))
      } else {  //there is only one department in this school.
        if(departments === undefined) { //if no departments in school
            school.department = []
            resolve(school)
        } else {
          addCoursesToDepartment(departments, departments.code, TERM)
            .then(department => school.department = department)
            .then(updatedSchool => resolve(updatedSchool))
            .catch(err => reject(err))
        }

      }
    })
  })
  const arr = []
  try {
    await Promise.each(schoolsPromises, x => arr.push(x)).catch(err => Promise.reject(err))
    //printJSON(arr)
    return arr
    // Promise.all(schoolsPromises).then(schools => printJSON(schools))
  } catch (e) {
    console.error('CAUGHT ERROR IN SCRAPER')
    console.error(e)
  }

}

async function addCoursesToDepartment(deptObject, dept, term) {
  await sleep(10000)
  return new Promise(
    (resolve, reject) => {
      var url = 'http://web-app.usc.edu/web/soc/api/classes/' + dept + '/' + term;
      request(url, function(err, response, body) {
        if (!err && response.statusCode == 200) {
          //parses the body text into a JSON object
          if(response.body.indexOf('ERROR')>-1){
            reject(new Error('Error: invalid course'));
          }else{
            let metaResponse = JSON.parse(body);
            let deptResponse = metaResponse.OfferedCourses.course;
            deptObject['courses'] = deptResponse;
            resolve(deptObject);
          }
        } else {
          reject(err);
        }
      });
    });
}

function printJSON(json) {
  console.log(JSON.stringify(json,null,2))
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
