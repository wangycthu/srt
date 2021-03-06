import json

from base import Base
from common import get
from setting import url

__all__ = ['Profile']

def conv(x):
  if x is None:
    return '0'
  return str(len(x))

class Profile(Base):
  name = 'profile'

  def _crawl(self, token_list, user_id, until_time=None):
    token = token_list[-1]
    response_profile = json.loads(get(url + 'profile/get', [
      ('access_token', token),
      ('userId', user_id),
    ]))
    if 'response' not in response_profile:
      return None
    response_user = json.loads(get(url + 'user/get', [
      ('access_token', token),
      ('userId', user_id),
    ]))
    if 'response' not in response_user:
      return None
    response = response_profile['response']
    response.update(response_user['response'])
    return response

  def _normalize(self, response):
    # TODO: density
    return [ json.dumps(response) + '\n' ]

  def _get_time(self, lines):
    return None
