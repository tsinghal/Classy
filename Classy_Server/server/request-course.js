import request from 'request'
import Promise from 'bluebird'
//gets a list of schools for a term
export async function getListOfSchools(term) {
  return new Promise(
    (resolve, reject) => {
      const url = `http://web-app.usc.edu/web/soc/api/depts/${term}`
      request(url, function(err, response, body) {
        if (!err && response.statusCode == 200) {
          //parses the body text into a JSON object
          if(response.body.indexOf('ERROR')>-1){
            reject(new Error('Error: invalid course'))
          }else{
            let metaResponse = JSON.parse(body)
            let deptResponse = metaResponse.department
            resolve(deptResponse);
          }
        } else {
          reject(err)
        }
      })
    }
  )
}
//returns all courses from a department: [{course},{course},{course}]
export async function requestDept(dept, term) {
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
            resolve(deptResponse);
          }
        } else {
          reject(err);
        }
      });
    }
  )
} //END requestDept()

//returns an individual course from Department
export function requestCourse(coursename, dept, term, cb) {
  requestDept(dept, term, function(err,courses) {
    if(err){
      return cb(err);
    }
    else{
      //filter out course we want
      let course = courses.filter(course => course.PublishedCourseID == coursename);
      cb(null,course);
    }
  });
}

//returns section data from a course
export function requestSection(coursename, dept, term, cb) {
  requestCourse(coursename, dept, term, function(err,course) {
    if(err){
      return cb(err);
    }
    //section array [lecture, lecture, quiz, etc]
    let section = course[0].CourseData.SectionData;
    cb(null,section);
  });
}
