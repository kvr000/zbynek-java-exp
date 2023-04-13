import React from 'react';
import logo from './logo.svg';
import './App.css';
import ProjectsPage from './projects/ProjectsPage';

function App() {
  return (
    <>
    <div className="App">
      <header className="App-header">
        <div className='container'><ProjectsPage/></div>
      </header>
      <div className='App-footer'>
        second div
      </div>
    </div>
    </>
  );
}

export default App;
