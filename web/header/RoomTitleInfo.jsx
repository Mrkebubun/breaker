import React, { Component } from 'react';
import Immutable from 'immutable';

import Config from '../config';

import RoomTitleActions from './RoomTitleActions';


export default class RoomIcon extends Component {
  render() {
    if (!this.props.room.get('name')) {
      return null;
    }

    const modMessageStyles = { marginLeft: '10px' };

    return (
      <ul className="nav navbar-nav hidden-sm" >
        <li className="m-t-xs m-b-xxs middle" >
          <span id="room-title" className="h4 m-n font-thin h4 text-black">
            <a href={`https://reddit.com/r/${this.props.room.get('name')}`} target="_blank">#{this.props.room.get('displayName')}</a></span>
          <RoomTitleActions room={this.props.room} userIsMod={this.props.userIsMod}/>
          <br/>
          <small style={modMessageStyles} id="room-modmessage" className="text-muted">
            {this.props.room.get('banner') ? this.props.room.get('banner') : Config.settings.default_banner}
          </small>
        </li>
      </ul>
    );
  }
}

RoomIcon.defaultProps = {
  room: Immutable.Map(),
  userIsMod: false
};
