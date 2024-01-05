This tool tests the concurrency handling of git operations (especially `git push`) in Artemis LocalVCS. The goal is to simulate concurrent pushes in order to find out how Artemis behaves in case two pushes reach the server at the same time.

When running the tool, the user can specify some options (i.e. the Artemis URL, admin account, user accounts, number of commits, number of users). If no input is entered, the tool uses the default values in the brackets.
The default values for the admin password and the password pattern can be specified in the environment variables `DEFAULT_ADMIN_PASSWORD` and `DEFAULT_PASSWORD_PATTERN`.

The simulation works as follows:
- Login the admin
- Create a course and a programming exercise
- Register the given number of users as instructors for the course
- All users clone the repository simultanously
- Repeat the following steps X times:
  - All users update theirs repository simultanously
  - All users change one file by adding the line `Commit n for user X` and commit
  - All users try to push their commits simultanously
  - The tool records which pushes are successful and which fail (with the respective error message)
- At the end, a summary of the successful pushes is printed
